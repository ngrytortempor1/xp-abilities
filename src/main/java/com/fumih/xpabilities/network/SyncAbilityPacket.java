package com.fumih.xpabilities.network;

import com.fumih.xpabilities.XpAbilities;
import com.fumih.xpabilities.ability.Ability;
import com.fumih.xpabilities.ability.PlayerAbilityData;
import com.fumih.xpabilities.client.ClientAbilityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.EnumSet;
import java.util.Set;

/**
 * サーバー→クライアント: アビリティデータの同期
 */
public record SyncAbilityPacket(Set<Ability> learned, Set<Ability> enabled) implements CustomPacketPayload {

    public static final Type<SyncAbilityPacket> TYPE =
            new Type<>(XpAbilities.id("sync_ability"));

    public static final StreamCodec<FriendlyByteBuf, SyncAbilityPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SyncAbilityPacket decode(FriendlyByteBuf buf) {
                    Set<Ability> learned = EnumSet.noneOf(Ability.class);
                    Set<Ability> enabled = EnumSet.noneOf(Ability.class);

                    int learnedSize = buf.readVarInt();
                    for (int i = 0; i < learnedSize; i++) {
                        Ability a = Ability.fromOrdinal(buf.readVarInt());
                        if (a != null) learned.add(a);
                    }

                    int enabledSize = buf.readVarInt();
                    for (int i = 0; i < enabledSize; i++) {
                        Ability a = Ability.fromOrdinal(buf.readVarInt());
                        if (a != null) enabled.add(a);
                    }

                    return new SyncAbilityPacket(learned, enabled);
                }

                @Override
                public void encode(FriendlyByteBuf buf, SyncAbilityPacket packet) {
                    buf.writeVarInt(packet.learned().size());
                    for (Ability a : packet.learned()) {
                        buf.writeVarInt(a.ordinal());
                    }

                    buf.writeVarInt(packet.enabled().size());
                    for (Ability a : packet.enabled()) {
                        buf.writeVarInt(a.ordinal());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * クライアント側でデータを反映
     */
    public static void handle(SyncAbilityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // クライアント側のキャッシュに保存
            ClientAbilityData.setData(packet.learned(), packet.enabled());
        });
    }

    /**
     * 指定プレイヤーにデータを送信するヘルパー
     */
    public static void sendToPlayer(ServerPlayer player) {
        PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);
        SyncAbilityPacket packet = new SyncAbilityPacket(
                data.getLearnedAbilities(),
                data.getEnabledAbilities()
        );
        PacketDistributor.sendToPlayer(player, packet);
    }
}
