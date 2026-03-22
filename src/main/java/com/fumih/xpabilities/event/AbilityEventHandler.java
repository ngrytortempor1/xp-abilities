package com.fumih.xpabilities.event;

import com.fumih.xpabilities.XpAbilities;
import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.ability.PlayerAbilityData;
import com.fumih.xpabilities.network.SyncAbilityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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

    /**
     * ダメージ軽減（鉄壁）
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player) {
            PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);
            if (data.isEnabled(Ability.IRON_WALL)) {
                // ダメージを10%カット
                float newDamage = event.getNewDamage() * 0.9f;
                event.setNewDamage(newDamage);
            }
        }
    }

    /**
     * プレイヤーティック処理
     * 移動速度、暗視、満腹度、Attribute Modifierの管理
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);

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
        // SATURATIONエフェクトを使用して満腹度の消費を軽減
        if (data.isEnabled(Ability.SATIATION)) {
            if (!player.hasEffect(MobEffects.SATURATION) ||
                    player.getEffect(MobEffects.SATURATION).getDuration() < 100) {
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 0, false, false));
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
        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.removeModifier(SWIFT_MODIFIER);

        var healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) healthAttr.removeModifier(VITALITY_MODIFIER);

        var jumpAttr = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpAttr != null) jumpAttr.removeModifier(LEAP_MODIFIER);
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
    }
}
