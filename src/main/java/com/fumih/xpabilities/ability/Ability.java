package com.fumih.xpabilities.ability;

/**
 * アビリティの定義
 * 各アビリティは名前、コスト（経験値ポイント）、効果タイプを持つ
 */
public enum Ability {
    // コスト500（基本アビリティ）
    IRON_WALL("iron_wall", 500, "鉄壁", "受けるダメージを10%カット"),
    SWIFT_FOOT("swift_foot", 500, "俊足", "移動速度が20%アップ"),
    MINER("miner", 500, "採掘師", "採掘速度が25%アップ"),
    LEAP("leap", 500, "跳躍", "ジャンプの高さが増加"),

    // コスト1000（上級アビリティ）
    VITALITY("vitality", 1000, "生命力", "最大HPがハート2個分増加"),
    NIGHT_VISION("night_vision", 1000, "夜目", "暗い場所でも視界が明るい"),
    SATIATION("satiation", 1000, "満腹", "満腹度の消費を50%軽減"),

    // コスト1500（最上級アビリティ）
    LUCKY_HAND("lucky_hand", 1500, "幸運の手", "モブからのドロップが増える");

    private final String id;
    private final int cost; // 経験値ポイント（レベルではない）
    private final String japaneseName;
    private final String description;

    Ability(String id, int cost, String japaneseName, String description) {
        this.id = id;
        this.cost = cost;
        this.japaneseName = japaneseName;
        this.description = description;
    }

    public String getId() { return id; }
    public int getCost() { return cost; }
    public String getJapaneseName() { return japaneseName; }
    public String getDescription() { return description; }

    /**
     * IDからアビリティを取得
     */
    public static Ability fromId(String id) {
        for (Ability ability : values()) {
            if (ability.id.equals(id)) {
                return ability;
            }
        }
        return null;
    }

    /**
     * ordinal値からアビリティを取得
     */
    public static Ability fromOrdinal(int ordinal) {
        Ability[] values = values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return null;
    }
}
