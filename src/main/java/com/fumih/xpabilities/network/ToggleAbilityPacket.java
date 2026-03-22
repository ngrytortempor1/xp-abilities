package com.fumih.xpabilities.network;

import com.fumih.xpabilities.XpAbilities;
import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.ability.PlayerAbilityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント→サーバー: アビリティのオン/オフ切替
 */
public record ToggleAbilityPacket(int abilityOrdinal) implements CustomPacketPayload {

    public static final Type<ToggleAbilityPacket> TYPE =
            new Type<>(XpAbilities.id("toggle_ability"));

    public static final StreamCodec<FriendlyByteBuf, ToggleAbilityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, ToggleAbilityPacket::abilityOrdinal,
                    ToggleAbilityPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * サーバー側でオン/オフ切替を処理
     */
    public static void handle(ToggleAbilityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Ability ability = Ability.fromOrdinal(packet.abilityOrdinal());
                if (ability == null) return;

                PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);

                // 習得していないアビリティは切り替えられない
                if (!data.hasLearned(ability)) return;

                boolean newState = data.toggleAbility(ability);
                XpAbilities.LOGGER.info("プレイヤー {} がアビリティ {} を {} にしました",
                        player.getName().getString(),
                        ability.getJapaneseName(),
                        newState ? "有効" : "無効");

                // クライアントに同期
                SyncAbilityPacket.sendToPlayer(player);
            }
        });
    }
}
