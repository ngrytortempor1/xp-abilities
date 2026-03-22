package com.fumih.xpabilities.event;

import com.fumih.xpabilities.XpAbilities;
import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.ability.PlayerAbilityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/**
 * 戦闘系アビリティの効果ハンドラ
 * 吸血・棘の鎧・クリティカルエッジを処理する
 */
@EventBusSubscriber(modid = XpAbilities.MOD_ID)
public class AbilityCombatHandler {

    /**
     * 吸血（ライフスティール）・棘の鎧（ソーン）
     * プレイヤーが攻撃を行った後、またはプレイヤーがダメージを受けた後に発動する
     */
    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        float actualDamage = event.getNewDamage();

        // --- 吸血 --- プレイヤーが攻撃者の場合、ダメージの15%をHP回復
        if (event.getSource().getEntity() instanceof Player attacker) {
            PlayerAbilityData attackerData = attacker.getData(XpAbilities.PLAYER_ABILITY_DATA);
            if (attackerData.isEnabled(Ability.LIFESTEAL)) {
                float healAmount = actualDamage * 0.15f;
                attacker.heal(healAmount);
            }
        }

        // --- 棘の鎧 --- プレイヤーがダメージを受けた場合、攻撃者に2ダメージ反射
        if (event.getEntity() instanceof Player defender) {
            PlayerAbilityData defenderData = defender.getData(XpAbilities.PLAYER_ABILITY_DATA);
            if (defenderData.isEnabled(Ability.THORNS)) {
                if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                    // thornsダメージソースで反射（無限ループ防止）
                    attacker.hurt(
                            defender.level().damageSources().thorns(defender),
                            2.0f
                    );
                }
            }
        }
    }

    /**
     * クリティカルエッジ
     * プレイヤーのクリティカルヒット時にダメージ倍率を1.5倍に増加する
     * （バニラの1.5倍 × 1.5 = 合計2.25倍相当）
     */
    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        // クリティカルヒット中のみ処理
        if (!event.isCriticalHit()) return;

        PlayerAbilityData data = event.getEntity().getData(XpAbilities.PLAYER_ABILITY_DATA);
        if (!data.isEnabled(Ability.CRITICAL_EDGE)) return;

        // 現在のダメージ倍率を1.5倍に増幅
        event.setDamageMultiplier(event.getDamageMultiplier() * 1.5f);
    }
}
