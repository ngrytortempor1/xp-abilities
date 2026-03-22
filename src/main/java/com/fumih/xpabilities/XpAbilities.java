package com.fumih.xpabilities;

import com.fumih.xpabilities.ability.PlayerAbilityData;
import com.fumih.xpabilities.network.LearnAbilityPacket;
import com.fumih.xpabilities.network.SyncAbilityPacket;
import com.fumih.xpabilities.network.ToggleAbilityPacket;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.function.Supplier;

/**
 * 経験値アビリティMODのメインクラス
 * 経験値を消費してプレイヤーが各種アビリティを習得できるシステムを提供する
 */
@Mod(XpAbilities.MOD_ID)
public class XpAbilities {
    public static final String MOD_ID = "xpabilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Data Attachment 登録
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    // プレイヤーのアビリティデータ（Codecベースで永続化）
    public static final Supplier<AttachmentType<PlayerAbilityData>> PLAYER_ABILITY_DATA =
            ATTACHMENT_TYPES.register("player_ability_data", () ->
                    AttachmentType.builder(PlayerAbilityData::new)
                            .serialize(PlayerAbilityData.CODEC.fieldOf("ability_data"))
                            .build()
            );

    public XpAbilities(IEventBus modEventBus) {
        LOGGER.info("XP Abilities MODを初期化中...");

        // Data Attachmentを登録
        ATTACHMENT_TYPES.register(modEventBus);

        // ネットワークパケットを登録
        modEventBus.addListener(this::registerPayloads);
    }

    /**
     * ネットワークペイロードの登録
     */
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MOD_ID).versioned("1.0.0");

        // クライアント→サーバー: アビリティ習得リクエスト
        registrar.playToServer(
                LearnAbilityPacket.TYPE,
                LearnAbilityPacket.STREAM_CODEC,
                LearnAbilityPacket::handle
        );

        // クライアント→サーバー: アビリティオン/オフ切替
        registrar.playToServer(
                ToggleAbilityPacket.TYPE,
                ToggleAbilityPacket.STREAM_CODEC,
                ToggleAbilityPacket::handle
        );

        // サーバー→クライアント: データ同期
        registrar.playToClient(
                SyncAbilityPacket.TYPE,
                SyncAbilityPacket.STREAM_CODEC,
                SyncAbilityPacket::handle
        );
    }

    /**
     * MODのリソースロケーションを作成するヘルパー
     */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
