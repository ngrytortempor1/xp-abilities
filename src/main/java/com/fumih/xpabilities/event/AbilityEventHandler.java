package com.fumih.xpabilities.event;

import com.fumih.xpabilities.XpAbilities;
import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.ability.PlayerAbilityData;
import com.fumih.xpabilities.network.SyncAbilityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * アビリティ効果を適用するイベントハンドラ
 * ゲームイベントに応じて習得済み＆有効なアビリティの効果を適用する
 */
@EventBusSubscriber(modid = XpAbilities.MOD_ID)
public class AbilityEventHandler {

    // Attribute Modifier用のリソースロケーション
    private static final net.minecraft.resources.ResourceLocation VITALITY_MODIFIER =
            XpAbilities.id("vitality_health_boost");
    private static final net.minecraft.resources.ResourceLocation LEAP_MODIFIER =
            XpAbilities.id("leap_jump_boost");
    private static final net.minecraft.resources.ResourceLocation SWIFT_MODIFIER =
            XpAbilities.id("swift_speed_boost");
    // 戦闘系Modifier
    private static final net.minecraft.resources.ResourceLocation BERSERK_MODIFIER =
            XpAbilities.id("berserk_attack_boost");
    private static final net.minecraft.resources.ResourceLocation PHALANX_MODIFIER =
            XpAbilities.id("phalanx_kb_resistance");
    // サバイバル系Modifier
    private static final net.minecraft.resources.ResourceLocation TOUGHNESS_MODIFIER =
            XpAbilities.id("toughness_armor_boost");

    // リジェネの回復間隔（60tick = 3秒）
    private static final int REGEN_INTERVAL = 60;
    // 磁石の引き寄せ範囲
    private static final double MAGNET_RANGE = 3.0;

    /**
     * ダメージ軽減（鉄壁・落下耐性・マジックシールド）
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);

        // --- 鉄壁（ダメージ10%カット）---
        if (data.isEnabled(Ability.IRON_WALL)) {
            event.setNewDamage(event.getNewDamage() * 0.9f);
        }

        // --- 落下耐性（落下ダメージ無効）---
        if (data.isEnabled(Ability.FEATHER_FALL)) {
            if (event.getSource().is(DamageTypes.FALL)) {
                event.setNewDamage(0f);
            }
        }

        // --- マジックシールド（火炎・毒・ウィザー等のダメージ50%軽減）---
        if (data.isEnabled(Ability.MAGIC_SHIELD)) {
            var source = event.getSource();
            if (source.is(DamageTypes.ON_FIRE)
                    || source.is(DamageTypes.IN_FIRE)
                    || source.is(DamageTypes.LAVA)
                    || source.is(DamageTypes.LIGHTNING_BOLT)
                    || source.is(DamageTypes.MAGIC)
                    || source.is(DamageTypes.WITHER)) {
                event.setNewDamage(event.getNewDamage() * 0.5f);
            }
        }
    }

    /**
     * プレイヤーティック処理
     * 移動速度、暗視、満腹度、Attribute Modifierの管理、リジェネ、磁石、ステルスなど
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);

        // ===== 基本系 =====

        // --- 俊足（移動速度UP）---
        updateAttributeModifier(player, Attributes.MOVEMENT_SPEED, SWIFT_MODIFIER,
                data.isEnabled(Ability.SWIFT_FOOT), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // --- 生命力（最大HP+4）---
        updateAttributeModifier(player, Attributes.MAX_HEALTH, VITALITY_MODIFIER,
                data.isEnabled(Ability.VITALITY), 4.0, AttributeModifier.Operation.ADD_VALUE);

        // --- 跳躍（ジャンプ力UP）---
        updateAttributeModifier(player, Attributes.JUMP_STRENGTH, LEAP_MODIFIER,
                data.isEnabled(Ability.LEAP), 0.3, AttributeModifier.Operation.ADD_VALUE);

        // --- 夜目（暗視）--- 効果が切れそうな時にリフレッシュ
        if (data.isEnabled(Ability.NIGHT_VISION)) {
            if (!player.hasEffect(MobEffects.NIGHT_VISION) ||
                    player.getEffect(MobEffects.NIGHT_VISION).getDuration() < 300) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            }
        }

        // --- 満腹（空腹軽減）---
        if (data.isEnabled(Ability.SATIATION)) {
            if (!player.hasEffect(MobEffects.SATURATION) ||
                    player.getEffect(MobEffects.SATURATION).getDuration() < 100) {
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 0, false, false));
            }
        }

        // ===== 戦闘系 =====

        // --- バーサーク（攻撃力30%UP）---
        updateAttributeModifier(player, Attributes.ATTACK_DAMAGE, BERSERK_MODIFIER,
                data.isEnabled(Ability.BERSERK), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // --- ファランクス（ノックバック耐性+50%）---
        updateAttributeModifier(player, Attributes.KNOCKBACK_RESISTANCE, PHALANX_MODIFIER,
                data.isEnabled(Ability.PHALANX), 0.5, AttributeModifier.Operation.ADD_VALUE);

        // ===== 魔法系 =====

        // --- リジェネ（3秒毎にHP回復）---
        if (data.isEnabled(Ability.REGEN)) {
            if (player.tickCount % REGEN_INTERVAL == 0) {
                // 1.0f = ハート0.5個分
                player.heal(1.0f);
            }
        }

        // --- ファイアレジスト（常時耐火）---
        if (data.isEnabled(Ability.FIRE_RESIST)) {
            if (!player.hasEffect(MobEffects.FIRE_RESISTANCE) ||
                    player.getEffect(MobEffects.FIRE_RESISTANCE).getDuration() < 300) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false));
            }
        }

        // --- 水中呼吸（常時水中呼吸）---
        if (data.isEnabled(Ability.WATER_BREATHING)) {
            if (!player.hasEffect(MobEffects.WATER_BREATHING) ||
                    player.getEffect(MobEffects.WATER_BREATHING).getDuration() < 300) {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, false, false));
            }
        }

        // ===== サバイバル系 =====

        // --- タフネス（防御力+4）---
        updateAttributeModifier(player, Attributes.ARMOR, TOUGHNESS_MODIFIER,
                data.isEnabled(Ability.TOUGHNESS), 4.0, AttributeModifier.Operation.ADD_VALUE);

        // --- 磁石（アイテム回収範囲2倍）--- 周囲のItemEntityをプレイヤーに引き寄せる
        if (data.isEnabled(Ability.MAGNET)) {
            AABB area = player.getBoundingBox().inflate(MAGNET_RANGE);
            List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, area);
            for (ItemEntity item : items) {
                if (!item.hasPickUpDelay()) {
                    double dx = player.getX() - item.getX();
                    double dy = player.getY() - item.getY();
                    double dz = player.getZ() - item.getZ();
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > 0.5) {
                        double speed = 0.15;
                        item.setDeltaMovement(
                                item.getDeltaMovement().x + dx / dist * speed,
                                item.getDeltaMovement().y + dy / dist * speed,
                                item.getDeltaMovement().z + dz / dist * speed
                        );
                    }
                }
            }
        }

        // --- ステルス（スニーク時に透明化）---
        if (data.isEnabled(Ability.STEALTH)) {
            if (player.isCrouching()) {
                if (!player.hasEffect(MobEffects.INVISIBILITY) ||
                        player.getEffect(MobEffects.INVISIBILITY).getDuration() < 60) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
                }
            }
        }
    }

    /**
     * Attribute Modifierの更新ヘルパー
     */
    private static void updateAttributeModifier(
            Player player,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            net.minecraft.resources.ResourceLocation modifierId,
            boolean shouldHave,
            double amount,
            AttributeModifier.Operation operation) {

        var attrInstance = player.getAttribute(attribute);
        if (attrInstance == null) return;

        boolean hasModifier = attrInstance.hasModifier(modifierId);
        if (shouldHave && !hasModifier) {
            attrInstance.addTransientModifier(
                    new AttributeModifier(modifierId, amount, operation));
        } else if (!shouldHave && hasModifier) {
            attrInstance.removeModifier(modifierId);
        }
    }

    /**
     * 死亡時にアビリティをリセット
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // 死亡の場合、新プレイヤーのデータはデフォルト（空）のまま
            // copyOnDeathを使わないので自動的にリセットされる

            // Attribute Modifierもクリア
            Player newPlayer = event.getEntity();
            removeAllAbilityModifiers(newPlayer);

            // クライアントに空データを同期
            if (newPlayer instanceof ServerPlayer serverPlayer) {
                SyncAbilityPacket.sendToPlayer(serverPlayer);
            }
        }
    }

    /**
     * ログイン時にデータを同期
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SyncAbilityPacket.sendToPlayer(serverPlayer);

            // ログイン時にAttribute Modifierを再適用
            PlayerAbilityData data = serverPlayer.getData(XpAbilities.PLAYER_ABILITY_DATA);
            reapplyModifiers(serverPlayer, data);
        }
    }

    /**
     * ディメンション変更時にデータを同期
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SyncAbilityPacket.sendToPlayer(serverPlayer);
        }
    }

    /**
     * リスポーン時にデータを同期
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SyncAbilityPacket.sendToPlayer(serverPlayer);
        }
    }

    /**
     * 全アビリティModifierを削除
     */
    private static void removeAllAbilityModifiers(Player player) {
        removeModifier(player, Attributes.MOVEMENT_SPEED, SWIFT_MODIFIER);
        removeModifier(player, Attributes.MAX_HEALTH, VITALITY_MODIFIER);
        removeModifier(player, Attributes.JUMP_STRENGTH, LEAP_MODIFIER);
        removeModifier(player, Attributes.ATTACK_DAMAGE, BERSERK_MODIFIER);
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, PHALANX_MODIFIER);
        removeModifier(player, Attributes.ARMOR, TOUGHNESS_MODIFIER);
    }

    private static void removeModifier(Player player,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
            net.minecraft.resources.ResourceLocation id) {
        var instance = player.getAttribute(attr);
        if (instance != null) instance.removeModifier(id);
    }

    /**
     * Modifierを再適用
     */
    private static void reapplyModifiers(Player player, PlayerAbilityData data) {
        updateAttributeModifier(player, Attributes.MOVEMENT_SPEED, SWIFT_MODIFIER,
                data.isEnabled(Ability.SWIFT_FOOT), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateAttributeModifier(player, Attributes.MAX_HEALTH, VITALITY_MODIFIER,
                data.isEnabled(Ability.VITALITY), 4.0, AttributeModifier.Operation.ADD_VALUE);
        updateAttributeModifier(player, Attributes.JUMP_STRENGTH, LEAP_MODIFIER,
                data.isEnabled(Ability.LEAP), 0.3, AttributeModifier.Operation.ADD_VALUE);
        updateAttributeModifier(player, Attributes.ATTACK_DAMAGE, BERSERK_MODIFIER,
                data.isEnabled(Ability.BERSERK), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateAttributeModifier(player, Attributes.KNOCKBACK_RESISTANCE, PHALANX_MODIFIER,
                data.isEnabled(Ability.PHALANX), 0.5, AttributeModifier.Operation.ADD_VALUE);
        updateAttributeModifier(player, Attributes.ARMOR, TOUGHNESS_MODIFIER,
                data.isEnabled(Ability.TOUGHNESS), 4.0, AttributeModifier.Operation.ADD_VALUE);
    }
}
