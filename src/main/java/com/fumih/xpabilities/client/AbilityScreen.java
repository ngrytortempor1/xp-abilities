package com.fumih.xpabilities.client;

import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.network.LearnAbilityPacket;
import com.fumih.xpabilities.network.ToggleAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

/**
 * アビリティ選択GUI画面
 * Kキーで開く画面。カテゴリタブ切替・スクロール対応。
 * アビリティの習得・オン/オフ切替ができる。
 *
 * 操作:
 * - 左クリック: 未習得のアビリティを習得（経験値消費）
 * - 右クリック: 習得済みアビリティのオン/オフ切替
 * - スクロール: リストのスクロール
 */
public class AbilityScreen extends Screen {

    // 画面レイアウト定数
    private static final int PANEL_WIDTH = 280;
    private static final int ENTRY_HEIGHT = 36;
    private static final int PADDING = 8;
    private static final int HEADER_HEIGHT = 30;
    private static final int TAB_HEIGHT = 22;
    private static final int MAX_VISIBLE_ENTRIES = 7;

    // カラー定数
    private static final int COLOR_BG = 0xCC1A1A2E;
    private static final int COLOR_HEADER = 0xFF16213E;
    private static final int COLOR_ENTRY_UNLEARNED = 0xAA2C2C54;
    private static final int COLOR_ENTRY_ENABLED = 0xAA1B5E20;
    private static final int COLOR_ENTRY_DISABLED = 0xAA4A1A1A;
    private static final int COLOR_TITLE = 0xFFE0E0FF;
    private static final int COLOR_DESC = 0xFFB0B0C8;
    private static final int COLOR_COST_OK = 0xFF4CAF50;
    private static final int COLOR_COST_NG = 0xFFFF5252;
    private static final int COLOR_HINT = 0xFF888899;
    private static final int COLOR_BORDER = 0xFF3A3A6A;
    private static final int COLOR_TAB_ACTIVE = 0xFF2A3A6A;
    private static final int COLOR_TAB_INACTIVE = 0xFF16213E;
    private static final int COLOR_TAB_TEXT_ACTIVE = 0xFFFFFFFF;
    private static final int COLOR_TAB_TEXT_INACTIVE = 0xFF888899;

    // 現在選択中のカテゴリ（nullは全表示）
    private Ability.Category selectedCategory = null;
    // スクロールオフセット（表示開始インデックス）
    private int scrollOffset = 0;

    public AbilityScreen() {
        super(Component.translatable("gui.xpabilities.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 現在のカテゴリに対応するアビリティリストを返す
     */
    private List<Ability> getFilteredAbilities() {
        if (selectedCategory == null) {
            return Arrays.asList(Ability.values());
        }
        return Arrays.stream(Ability.values())
                .filter(a -> a.getCategory() == selectedCategory)
                .toList();
    }

    /**
     * スクロールオフセットをアビリティリストの範囲内にクランプする
     */
    private int clampScroll(int offset, int listSize) {
        return Math.max(0, Math.min(offset, Math.max(0, listSize - MAX_VISIBLE_ENTRIES)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        List<Ability> abilities = getFilteredAbilities();
        int visibleCount = Math.min(abilities.size(), MAX_VISIBLE_ENTRIES);
        int panelX = (this.width - PANEL_WIDTH) / 2;
        // パネル高さ = ヘッダー + タブ + エントリ + 操作説明
        int panelHeight = HEADER_HEIGHT + TAB_HEIGHT + visibleCount * (ENTRY_HEIGHT + 2) + PADDING * 2 + 20;
        int panelY = (this.height - panelHeight) / 2;

        // パネル背景
        graphics.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + panelHeight + 2, COLOR_BORDER);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, COLOR_BG);

        // ヘッダー
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + HEADER_HEIGHT, COLOR_HEADER);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.xpabilities.title"),
                panelX + PANEL_WIDTH / 2, panelY + 10, COLOR_TITLE);

        // カテゴリタブ描画
        renderCategoryTabs(graphics, panelX, panelY + HEADER_HEIGHT, mouseX, mouseY);

        // 経験値ポイント表示
        int playerXp = getPlayerTotalXp();
        String xpText = "\u00A76経験値: " + playerXp + " pt";
        graphics.drawString(this.font, xpText, panelX + PADDING,
                panelY + HEADER_HEIGHT + TAB_HEIGHT + PADDING / 2, 0xFFFFD700);

        int listStartY = panelY + HEADER_HEIGHT + TAB_HEIGHT + PADDING + 10;
        int entryY = listStartY;

        // スクロール範囲を制限
        scrollOffset = clampScroll(scrollOffset, abilities.size());

        // 表示するエントリをスクロールオフセットから描画
        for (int i = scrollOffset; i < Math.min(scrollOffset + MAX_VISIBLE_ENTRIES, abilities.size()); i++) {
            Ability ability = abilities.get(i);
            renderAbilityEntry(graphics, ability, panelX, entryY, playerXp, mouseX, mouseY);
            entryY += ENTRY_HEIGHT + 2;
        }

        // スクロールインジケーター（スクロール可能な場合）
        if (abilities.size() > MAX_VISIBLE_ENTRIES) {
            int scrollBarX = panelX + PANEL_WIDTH - 6;
            int scrollBarHeight = visibleCount * (ENTRY_HEIGHT + 2);
            int thumbH = Math.max(20, scrollBarHeight * MAX_VISIBLE_ENTRIES / abilities.size());
            int maxOffset = abilities.size() - MAX_VISIBLE_ENTRIES;
            int thumbY = listStartY + (scrollBarHeight - thumbH) * scrollOffset / Math.max(1, maxOffset);
            graphics.fill(scrollBarX, listStartY, scrollBarX + 4, listStartY + scrollBarHeight, 0x44FFFFFF);
            graphics.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbH, 0xAAFFFFFF);
        }

        // 操作説明
        graphics.drawCenteredString(this.font,
                "\u00A78左クリック: 習得  |  右クリック: ON/OFF切替",
                panelX + PANEL_WIDTH / 2, entryY + 4, COLOR_HINT);
    }

    /**
     * カテゴリタブの描画
     */
    private void renderCategoryTabs(GuiGraphics graphics, int panelX, int tabY, int mouseX, int mouseY) {
        // タブ定義: null=全て、各カテゴリ
        String[] tabLabels = {"全て", "基本", "戦闘", "魔法", "サバイバル"};
        Ability.Category[] tabCategories = {null, Ability.Category.BASIC, Ability.Category.COMBAT,
                Ability.Category.MAGIC, Ability.Category.SURVIVAL};

        int tabCount = tabLabels.length;
        int tabWidth = PANEL_WIDTH / tabCount;

        for (int i = 0; i < tabCount; i++) {
            int tabX = panelX + i * tabWidth;
            boolean active = (selectedCategory == tabCategories[i]);
            int bgColor = active ? COLOR_TAB_ACTIVE : COLOR_TAB_INACTIVE;
            int textColor = active ? COLOR_TAB_TEXT_ACTIVE : COLOR_TAB_TEXT_INACTIVE;

            graphics.fill(tabX, tabY, tabX + tabWidth - 1, tabY + TAB_HEIGHT, bgColor);
            graphics.drawCenteredString(this.font, tabLabels[i],
                    tabX + tabWidth / 2, tabY + (TAB_HEIGHT - 8) / 2, textColor);
        }
    }

    /**
     * 1エントリの描画
     */
    private void renderAbilityEntry(GuiGraphics graphics, Ability ability,
            int panelX, int entryY, int playerXp, int mouseX, int mouseY) {
        boolean learned = ClientAbilityData.hasLearned(ability);
        boolean enabled = ClientAbilityData.isEnabled(ability);
        boolean canAfford = playerXp >= ability.getCost();
        boolean hovered = mouseX >= panelX + PADDING && mouseX <= panelX + PANEL_WIDTH - PADDING
                && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT;

        int bgColor;
        if (learned && enabled) {
            bgColor = COLOR_ENTRY_ENABLED;
        } else if (learned) {
            bgColor = COLOR_ENTRY_DISABLED;
        } else {
            bgColor = COLOR_ENTRY_UNLEARNED;
        }
        if (hovered) {
            bgColor = brighten(bgColor, 30);
        }

        graphics.fill(panelX + PADDING, entryY, panelX + PANEL_WIDTH - PADDING, entryY + ENTRY_HEIGHT, bgColor);

        // アビリティ名
        graphics.drawString(this.font, ability.getJapaneseName(),
                panelX + PADDING + 4, entryY + 4, 0xFFFFFFFF);

        // コスト or 状態表示
        if (!learned) {
            String costText = ability.getCost() + " pt";
            int costColor = canAfford ? COLOR_COST_OK : COLOR_COST_NG;
            int costX = panelX + PANEL_WIDTH - PADDING - 4 - this.font.width(costText);
            graphics.drawString(this.font, costText, costX, entryY + 4, costColor);
        } else {
            String statusText = enabled ? "\u00A7a[ON]" : "\u00A7c[OFF]";
            int statusX = panelX + PANEL_WIDTH - PADDING - 4 - this.font.width(enabled ? "[ON]" : "[OFF]");
            graphics.drawString(this.font, statusText, statusX, entryY + 4, 0xFFFFFFFF);
        }

        // 説明文
        graphics.drawString(this.font, "\u00A77" + ability.getDescription(),
                panelX + PADDING + 4, entryY + 16, COLOR_DESC);

        // ホバー時のヒント
        if (hovered) {
            String hint;
            if (!learned) {
                hint = canAfford ? "\u00A7e左クリックで習得" : "\u00A7c経験値が足りません";
            } else {
                hint = "\u00A7e右クリックでON/OFF切替";
            }
            graphics.drawString(this.font, hint,
                    panelX + PADDING + 4, entryY + ENTRY_HEIGHT - 10, COLOR_HINT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Ability> abilities = getFilteredAbilities();
        int visibleCount = Math.min(abilities.size(), MAX_VISIBLE_ENTRIES);
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelHeight = HEADER_HEIGHT + TAB_HEIGHT + visibleCount * (ENTRY_HEIGHT + 2) + PADDING * 2 + 20;
        int panelY = (this.height - panelHeight) / 2;

        // カテゴリタブクリック判定
        int tabY = panelY + HEADER_HEIGHT;
        if (mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT) {
            String[] tabLabels = {"全て", "基本", "戦闘", "魔法", "サバイバル"};
            Ability.Category[] tabCategories = {null, Ability.Category.BASIC, Ability.Category.COMBAT,
                    Ability.Category.MAGIC, Ability.Category.SURVIVAL};
            int tabWidth = PANEL_WIDTH / tabLabels.length;
            for (int i = 0; i < tabLabels.length; i++) {
                int tabX = panelX + i * tabWidth;
                if (mouseX >= tabX && mouseX <= tabX + tabWidth) {
                    selectedCategory = tabCategories[i];
                    scrollOffset = 0;
                    return true;
                }
            }
        }

        // エントリクリック判定
        int listStartY = panelY + HEADER_HEIGHT + TAB_HEIGHT + PADDING + 10;
        int entryY = listStartY;

        scrollOffset = clampScroll(scrollOffset, abilities.size());

        for (int i = scrollOffset; i < Math.min(scrollOffset + MAX_VISIBLE_ENTRIES, abilities.size()); i++) {
            Ability ability = abilities.get(i);
            if (mouseX >= panelX + PADDING && mouseX <= panelX + PANEL_WIDTH - PADDING
                    && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT) {

                if (button == 0) {
                    // 左クリック: アビリティ習得
                    if (!ClientAbilityData.hasLearned(ability)) {
                        int playerXp = getPlayerTotalXp();
                        if (playerXp >= ability.getCost()) {
                            Minecraft.getInstance().getConnection().send(new LearnAbilityPacket(ability.ordinal()));
                        }
                    }
                    return true;
                } else if (button == 1) {
                    // 右クリック: オン/オフ切替
                    if (ClientAbilityData.hasLearned(ability)) {
                        Minecraft.getInstance().getConnection().send(new ToggleAbilityPacket(ability.ordinal()));
                    }
                    return true;
                }
            }
            entryY += ENTRY_HEIGHT + 2;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<Ability> abilities = getFilteredAbilities();
        if (scrollY != 0) {
            scrollOffset = clampScroll(scrollOffset - (int) Math.signum(scrollY), abilities.size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * プレイヤーの総経験値ポイントを取得（クライアント側）
     */
    private int getPlayerTotalXp() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        return getXpForLevel(player.experienceLevel)
                + (int)(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /**
     * 指定レベルまでに必要な累計経験値
     */
    private int getXpForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int)(2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int)(4.5 * level * level - 162.5 * level + 2220);
        }
    }

    /**
     * 色を明るくするヘルパー
     */
    private static int brighten(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
