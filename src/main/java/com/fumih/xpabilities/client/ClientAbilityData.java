package com.fumih.xpabilities.client;

import com.fumih.xpabilities.ability.Ability;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * クライアント側のアビリティデータキャッシュ
 * サーバーから同期されたデータを保持し、GUI表示に使用する
 */
public class ClientAbilityData {
    private static Set<Ability> learnedAbilities = EnumSet.noneOf(Ability.class);
    private static Set<Ability> enabledAbilities = EnumSet.noneOf(Ability.class);

    /**
     * サーバーから同期されたデータを設定
     */
    public static void setData(Set<Ability> learned, Set<Ability> enabled) {
        learnedAbilities = EnumSet.noneOf(Ability.class);
        learnedAbilities.addAll(learned);
        enabledAbilities = EnumSet.noneOf(Ability.class);
        enabledAbilities.addAll(enabled);
    }

    public static boolean hasLearned(Ability ability) {
        return learnedAbilities.contains(ability);
    }

    public static boolean isEnabled(Ability ability) {
        return learnedAbilities.contains(ability) && enabledAbilities.contains(ability);
    }

    public static Set<Ability> getLearnedAbilities() {
        return Collections.unmodifiableSet(learnedAbilities);
    }

    public static Set<Ability> getEnabledAbilities() {
        return Collections.unmodifiableSet(enabledAbilities);
    }

    /**
     * データをクリア（ログアウト時）
     */
    public static void clear() {
        learnedAbilities.clear();
        enabledAbilities.clear();
    }
}
