package com.fumih.xpabilities.event;

import com.fumih.xpabilities.XpAbilities;
import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.ability.PlayerAbilityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Optional;

/**
 * 採掘速度・ドロップ増加・経験値ブーストの効果ハンドラ
 * （メインのAbilityEventHandlerと分離して可読性を確保）
 */
@EventBusSubscriber(modid = XpAbilities.MOD_ID)
public class AbilityMiningHandler {

    /**
     * 採掘速度UP（採掘師）
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);
        if (data.isEnabled(Ability.MINER)) {
            // 採掘速度を25%アップ
            event.setNewSpeed(event.getNewSpeed() * 1.25f);
        }
    }

    /**
     * ドロップ増加（幸運の手）
     * モブが死亡した際にドロップアイテムを複製する
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);
            if (data.isEnabled(Ability.LUCKY_HAND)) {
                // 各ドロップアイテムの数を1.5倍（切り上げ）にする
                event.getDrops().forEach(itemEntity -> {
                    var stack = itemEntity.getItem();
                    int bonus = (int) Math.ceil(stack.getCount() * 0.5);
                    stack.grow(bonus);
                });
            }
        }
    }

    /**
     * 経験値ブースト（自動経験値）
     * 敵撃破時の経験値を1.5倍にする
     */
    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() != null) {
            Player player = event.getAttackingPlayer();
            PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);
            if (data.isEnabled(Ability.XP_BOOST)) {
                // 経験値を1.5倍に
                int boosted = (int) (event.getDroppedExperience() * 1.5);
                event.setDroppedExperience(boosted);
            }
        }
    }

    /**
     * オートスメルト（鉱石採掘時に精錬済みアイテムでドロップ）
     */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        // プレイヤーが採掘した場合のみ
        if (!(event.getBreaker() instanceof Player player)) return;
        PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);
        if (!data.isEnabled(Ability.AUTO_SMELT)) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        // ドロップアイテムを精錬結果に置換（コンテナを再利用してパフォーマンス向上）
        net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(1);
        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack stack = itemEntity.getItem();
            // コンテナにスタックをセットして精錬レシピを検索
            container.setItem(0, stack);
            Optional<SmeltingRecipe> recipe = serverLevel.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, container, serverLevel)
                    .map(holder -> holder.value());
            recipe.ifPresent(smeltingRecipe -> {
                ItemStack result = smeltingRecipe.getResultItem(serverLevel.registryAccess());
                if (!result.isEmpty()) {
                    // ドロップスタックの数量を維持しながら精錬結果に置換
                    int count = stack.getCount();
                    itemEntity.setItem(result.copyWithCount(count));
                }
            });
        }
    }
}
