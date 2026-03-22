package com.fumih.xpabilities.client;

import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.network.LearnAbilityPacket;
import com.fumih.xpabilities.network.ToggleAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * アビリティ選択GUI画面
 * Kキーで開く画面。アビリティの習得・オン/オフ切替ができる。
 *
 * 操作:
 * - 左クリック: 未習得のアビリティを習得（経験値消費）
 * - 右クリック: 習得済みアビリティのオン/オフ切替
 */
public class AbilityScreen extends Screen {

    // 画面レイアウト定数
    private static final int PANEL_WIDTH = 260;
    private static final int ENTRY_HEIGHT = 36;
    private static final int PADDING = 8;
    private static final int HEADER_HEIGHT = 30;

    // カラー定数
    private static final int COLOR_BG = 0xCC1A1A2E;          // 背景（半透明ダーク）
    private static final int COLOR_HEADER = 0xFF16213E;       // ヘッダー背景
    private static final int COLOR_ENTRY_UNLEARNED = 0xAA2C2C54; // 未習得
    private static final int COLOR_ENTRY_ENABLED = 0xAA1B5E20;  // 有効（緑）
    private static final int COLOR_ENTRY_DISABLED = 0xAA4A1A1A; // 無効（赤っぽい）
    private static final int COLOR_TITLE = 0xFFE0E0FF;        // タイトル色
    private static final int COLOR_DESC = 0xFFB0B0C8;         // 説明色
    private static final int COLOR_COST_OK = 0xFF4CAF50;      // コスト（足りる）
    private static final int COLOR_COST_NG = 0xFFFF5252;      // コスト（足りない）
    private static final int COLOR_HINT = 0xFF888899;         // ヒントテキスト
    private static final int COLOR_BORDER = 0xFF3A3A6A;       // ボーダー

    public AbilityScreen() {
        super(Component.translatable("gui.xpabilities.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 薄暗い背景を描画
        super.render(graphics, mouseX, mouseY, partialTick);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int totalEntries = Ability.values().length;
        int panelHeight = HEADER_HEIGHT + totalEntries * (ENTRY_HEIGHT + 2) + PADDING * 2 + 20;
        int panelY = (this.height - panelHeight) / 2;

        // パネル背景
        graphics.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + panelHeight + 2, COLOR_BORDER);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, COLOR_BG);

        // ヘッダー
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + HEADER_HEIGHT, COLOR_HEADER);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.xpabilities.title"),
                panelX + PANEL_WIDTH / 2, panelY + 10, COLOR_TITLE);

        // 経験値ポイント表示
        int playerXp = getPlayerTotalXp();
        String xpText = "\u00A76経験値: " + playerXp + " pt";
        graphics.drawString(this.font, xpText, panelX + PADDING, panelY + HEADER_HEIGHT + PADDING, 0xFFFFD700);

        int entryY = panelY + HEADER_HEIGHT + PADDING + 14;

        // 各アビリティのエントリを描画
        for (Ability ability : Ability.values()) {
            boolean learned = ClientAbilityData.hasLearned(ability);
            boolean enabled = ClientAbilityData.isEnabled(ability);
            boolean canAfford = playerXp >= ability.getCost();
            boolean hovered = mouseX >= panelX + PADDING && mouseX <= panelX + PANEL_WIDTH - PADDING
                    && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT;

            // エントリ背景色
            int bgColor;
            if (learned && enabled) {
                bgColor = COLOR_ENTRY_ENABLED;
            } else if (learned) {
                bgColor = COLOR_ENTRY_DISABLED;
            } else {
                bgColor = COLOR_ENTRY_UNLEARNED;
            }

            // ホバー時に少し明るく
            if (hovered) {
                bgColor = brighten(bgColor, 30);
            }

            // エントリ背景
            graphics.fill(panelX + PADDING, entryY, panelX + PANEL_WIDTH - PADDING, entryY + ENTRY_HEIGHT, bgColor);

            // アビリティ名
            String name = ability.getJapaneseName();
            graphics.drawString(this.font, name, panelX + PADDING + 4, entryY + 4, 0xFFFFFFFF);

            // コスト表示
            if (!learned) {
                String costText = ability.getCost() + " pt";
                int costColor = canAfford ? COLOR_COST_OK : COLOR_COST_NG;
                int costX = panelX + PANEL_WIDTH - PADDING - 4 - this.font.width(costText);
                graphics.drawString(this.font, costText, costX, entryY + 4, costColor);
            } else {
                // 状態表示
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

            entryY += ENTRY_HEIGHT + 2;
        }

        // 操作説明
        graphics.drawCenteredString(this.font,
                "\u00A78左クリック: 習得  |  右クリック: ON/OFF切替",
                panelX + PANEL_WIDTH / 2, entryY + 4, COLOR_HINT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int totalEntries = Ability.values().length;
        int panelHeight = HEADER_HEIGHT + totalEntries * (ENTRY_HEIGHT + 2) + PADDING * 2 + 20;
        int panelY = (this.height - panelHeight) / 2;
        int entryY = panelY + HEADER_HEIGHT + PADDING + 14;

        for (Ability ability : Ability.values()) {
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
