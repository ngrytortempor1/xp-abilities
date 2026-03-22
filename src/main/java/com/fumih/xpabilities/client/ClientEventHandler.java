package com.fumih.xpabilities.client;

import com.fumih.xpabilities.XpAbilities;
import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.network.DoubleJumpPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * クライアント側イベントハンドラ
 * キーバインドの登録と処理を行う
 */
public class ClientEventHandler {

    // アビリティ画面を開くキー（デフォルト: K）
    public static final KeyMapping OPEN_ABILITY_SCREEN = new KeyMapping(
            "key.xpabilities.open_screen",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.xpabilities"
    );

    // 二段ジャンプ用: 前回のジャンプキー状態と空中フラグを管理
    private static boolean wasJumpPressed = false;
    private static boolean doubleJumpUsed = false;

    /**
     * キーマッピング登録（MODバスイベント）
     */
    @EventBusSubscriber(modid = XpAbilities.MOD_ID, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_ABILITY_SCREEN);
        }
    }

    /**
     * クライアントティックでキー入力を監視
     */
    @EventBusSubscriber(modid = XpAbilities.MOD_ID, value = Dist.CLIENT)
    public static class GameBusEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();

            // --- アビリティ画面を開く ---
            while (OPEN_ABILITY_SCREEN.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new AbilityScreen());
                }
            }

            // --- 二段ジャンプ処理 ---
            if (mc.player == null || mc.screen != null) return;

            // 地上・水中・クライミング中はフラグリセット
            if (mc.player.onGround() || mc.player.isInWater() || mc.player.onClimbable()) {
                doubleJumpUsed = false;
                wasJumpPressed = false;
                return;
            }

            // 二段ジャンプアビリティが有効かチェック（クライアント側のデータで判断）
            if (!ClientAbilityData.isEnabled(Ability.DOUBLE_JUMP)) {
                wasJumpPressed = false;
                return;
            }

            boolean jumpPressed = mc.options.keyJump.isDown();

            // ジャンプキーが押された瞬間（前回未押下→今回押下）
            if (jumpPressed && !wasJumpPressed && !doubleJumpUsed) {
                doubleJumpUsed = true;
                // サーバーに二段ジャンプパケットを送信
                mc.getConnection().send(new DoubleJumpPacket());
            }

            wasJumpPressed = jumpPressed;
        }
    }
}
