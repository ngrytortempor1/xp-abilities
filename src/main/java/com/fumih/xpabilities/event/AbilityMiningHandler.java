package com.fumih.xpabilities.event;

import com.fumih.xpabilities.XpAbilities;
import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.ability.PlayerAbilityData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 採掘速度・ドロップ増加の効果ハンドラ
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
}
