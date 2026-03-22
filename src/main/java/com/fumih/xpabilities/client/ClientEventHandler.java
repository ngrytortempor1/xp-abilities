package com.fumih.xpabilities.client;

import com.fumih.xpabilities.XpAbilities;
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
            while (OPEN_ABILITY_SCREEN.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null) {
                    mc.setScreen(new AbilityScreen());
                }
            }
        }
    }
}
