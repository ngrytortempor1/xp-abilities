package com.fumih.xpabilities.network;

import com.fumih.xpabilities.XpAbilities;
import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.ability.PlayerAbilityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント→サーバー: 二段ジャンプリクエスト
 * プレイヤーが空中でジャンプキーを押した際に送信される
 */
public record DoubleJumpPacket() implements CustomPacketPayload {

    public static final Type<DoubleJumpPacket> TYPE =
            new Type<>(XpAbilities.id("double_jump"));

    public static final StreamCodec<FriendlyByteBuf, DoubleJumpPacket> STREAM_CODEC =
            StreamCodec.unit(new DoubleJumpPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * サーバー側で二段ジャンプを処理
     * プレイヤーに上方向の速度を与える
     */
    public static void handle(DoubleJumpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);
                if (data.isEnabled(Ability.DOUBLE_JUMP)) {
                    // 上方向に速度を付与（バニラのジャンプと同等）
                    player.setDeltaMovement(
                            player.getDeltaMovement().x,
                            0.42,
                            player.getDeltaMovement().z
                    );
                    player.hurtMarked = true;
                }
            }
        });
    }
}
