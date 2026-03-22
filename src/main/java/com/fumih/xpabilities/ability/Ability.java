package com.fumih.xpabilities.ability;

/**
 * アビリティの定義
 * 各アビリティは名前、コスト（経験値ポイント）、カテゴリ、効果タイプを持つ
 */
public enum Ability {

    // ===== 基本カテゴリ（BASIC）=====
    IRON_WALL("iron_wall", 500, Category.BASIC, "鉄壁", "受けるダメージを10%カット"),
    SWIFT_FOOT("swift_foot", 500, Category.BASIC, "俊足", "移動速度が20%アップ"),
    MINER("miner", 500, Category.BASIC, "採掘師", "採掘速度が25%アップ"),
    LEAP("leap", 500, Category.BASIC, "跳躍", "ジャンプの高さが増加"),
    VITALITY("vitality", 1000, Category.BASIC, "生命力", "最大HPがハート2個分増加"),
    NIGHT_VISION("night_vision", 1000, Category.BASIC, "夜目", "暗い場所でも視界が明るい"),
    SATIATION("satiation", 1000, Category.BASIC, "満腹", "満腹度の消費を50%軽減"),
    LUCKY_HAND("lucky_hand", 1500, Category.BASIC, "幸運の手", "モブからのドロップが増える"),

    // ===== 戦闘カテゴリ（COMBAT）=====
    BERSERK("berserk", 800, Category.COMBAT, "バーサーク", "攻撃力が30%アップ"),
    PHALANX("phalanx", 1000, Category.COMBAT, "ファランクス", "ノックバック耐性+50%"),
    LIFESTEAL("lifesteal", 1200, Category.COMBAT, "吸血", "攻撃時にダメージの15%をHP回復"),
    CRITICAL_EDGE("critical_edge", 1500, Category.COMBAT, "クリティカルエッジ", "クリティカル時のダメージ1.5倍"),
    THORNS("thorns", 1000, Category.COMBAT, "棘の鎧", "攻撃してきた敵に2ダメージ反射"),

    // ===== 魔法カテゴリ（MAGIC）=====
    REGEN("regen", 1000, Category.MAGIC, "リジェネ", "3秒毎にHP0.5個分自動回復"),
    MAGIC_SHIELD("magic_shield", 1200, Category.MAGIC, "マジックシールド", "火炎・毒・ウィザー等のダメージ50%軽減"),
    FIRE_RESIST("fire_resist", 800, Category.MAGIC, "ファイアレジスト", "常時耐火属性"),
    WATER_BREATHING("water_breathing", 800, Category.MAGIC, "水中呼吸", "常時水中で呼吸可能"),
    XP_BOOST("xp_boost", 2000, Category.MAGIC, "自動経験値", "敵撃破時の経験値+50%"),

    // ===== サバイバルカテゴリ（SURVIVAL）=====
    TOUGHNESS("toughness", 1500, Category.SURVIVAL, "タフネス", "防御力+4"),
    FEATHER_FALL("feather_fall", 600, Category.SURVIVAL, "落下耐性", "落下ダメージ無効"),
    MAGNET("magnet", 1000, Category.SURVIVAL, "磁石", "アイテム回収範囲が2倍"),
    STEALTH("stealth", 1200, Category.SURVIVAL, "ステルス", "スニーク時に透明化"),
    DOUBLE_JUMP("double_jump", 1500, Category.SURVIVAL, "二段ジャンプ", "空中で追加ジャンプ1回"),
    AUTO_SMELT("auto_smelt", 1000, Category.SURVIVAL, "オートスメルト", "鉱石採掘時に精錬済みアイテムでドロップ");

    /**
     * アビリティのカテゴリ
     */
    public enum Category {
        BASIC("基本"),
        COMBAT("戦闘"),
        MAGIC("魔法"),
        SURVIVAL("サバイバル");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String id;
    private final int cost; // 経験値ポイント（レベルではない）
    private final Category category; // カテゴリ
    private final String japaneseName;
    private final String description;

    Ability(String id, int cost, Category category, String japaneseName, String description) {
        this.id = id;
        this.cost = cost;
        this.category = category;
        this.japaneseName = japaneseName;
        this.description = description;
    }

    public String getId() { return id; }
    public int getCost() { return cost; }
    public Category getCategory() { return category; }
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
