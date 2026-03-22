package com.fumih.xpabilities.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * プレイヤーのアビリティデータ
 * 習得済みアビリティと有効/無効状態を管理する
 * Data Attachmentとしてプレイヤーに紐付けられる
 *
 * NeoForge 1.21.8ではCodecベースのシリアライゼーションを使用
 */
public class PlayerAbilityData {
    // 習得済みアビリティ
    private final Set<Ability> learnedAbilities = EnumSet.noneOf(Ability.class);
    // 有効なアビリティ（習得済みのうち、オンになっているもの）
    private final Set<Ability> enabledAbilities = EnumSet.noneOf(Ability.class);

    // Codecの定義（NeoForge Data Attachment用）
    public static final Codec<PlayerAbilityData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.listOf().fieldOf("learned").forGetter(data -> {
                        List<String> list = new ArrayList<>();
                        for (Ability a : data.learnedAbilities) list.add(a.getId());
                        return list;
                    }),
                    Codec.STRING.listOf().fieldOf("enabled").forGetter(data -> {
                        List<String> list = new ArrayList<>();
                        for (Ability a : data.enabledAbilities) list.add(a.getId());
                        return list;
                    })
            ).apply(instance, (learned, enabled) -> {
                PlayerAbilityData data = new PlayerAbilityData();
                for (String id : learned) {
                    Ability a = Ability.fromId(id);
                    if (a != null) data.learnedAbilities.add(a);
                }
                for (String id : enabled) {
                    Ability a = Ability.fromId(id);
                    if (a != null && data.learnedAbilities.contains(a)) {
                        data.enabledAbilities.add(a);
                    }
                }
                return data;
            })
    );

    public PlayerAbilityData() {
        // デフォルトコンストラクタ（空の状態）
    }

    /**
     * アビリティを習得する
     * 習得時にデフォルトで有効化する
     */
    public boolean learnAbility(Ability ability) {
        if (learnedAbilities.add(ability)) {
            enabledAbilities.add(ability);
            return true;
        }
        return false;
    }

    /**
     * アビリティが習得済みかチェック
     */
    public boolean hasLearned(Ability ability) {
        return learnedAbilities.contains(ability);
    }

    /**
     * アビリティが有効（オン）かチェック
     * 習得済みかつ有効な場合のみtrue
     */
    public boolean isEnabled(Ability ability) {
        return learnedAbilities.contains(ability) && enabledAbilities.contains(ability);
    }

    /**
     * アビリティの有効/無効を切り替える
     * @return 変更後の状態（true=有効、false=無効）
     */
    public boolean toggleAbility(Ability ability) {
        if (!learnedAbilities.contains(ability)) {
            return false;
        }
        if (enabledAbilities.contains(ability)) {
            enabledAbilities.remove(ability);
            return false;
        } else {
            enabledAbilities.add(ability);
            return true;
        }
    }

    /**
     * 全データをリセット（死亡時に呼ばれる）
     */
    public void reset() {
        learnedAbilities.clear();
        enabledAbilities.clear();
    }

    /**
     * 習得済みアビリティの読み取り専用セット
     */
    public Set<Ability> getLearnedAbilities() {
        return Collections.unmodifiableSet(learnedAbilities);
    }

    /**
     * 有効なアビリティの読み取り専用セット
     */
    public Set<Ability> getEnabledAbilities() {
        return Collections.unmodifiableSet(enabledAbilities);
    }
}
