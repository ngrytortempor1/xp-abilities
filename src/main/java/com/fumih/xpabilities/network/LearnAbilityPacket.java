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
 * クライアント→サーバー: アビリティ習得リクエスト
 * プレイヤーがGUIからアビリティを習得するときに送信される
 */
public record LearnAbilityPacket(int abilityOrdinal) implements CustomPacketPayload {

    public static final Type<LearnAbilityPacket> TYPE =
            new Type<>(XpAbilities.id("learn_ability"));

    public static final StreamCodec<FriendlyByteBuf, LearnAbilityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, LearnAbilityPacket::abilityOrdinal,
                    LearnAbilityPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * サーバー側でアビリティ習得を処理
     */
    public static void handle(LearnAbilityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Ability ability = Ability.fromOrdinal(packet.abilityOrdinal());
                if (ability == null) return;

                PlayerAbilityData data = player.getData(XpAbilities.PLAYER_ABILITY_DATA);

                // 既に習得済みなら何もしない
                if (data.hasLearned(ability)) return;

                // 経験値ポイントのチェック
                int totalXp = getPlayerTotalXp(player);
                if (totalXp < ability.getCost()) return;

                // 経験値を消費
                addPlayerXp(player, -ability.getCost());

                // アビリティを習得
                data.learnAbility(ability);

                // クライアントに同期
                SyncAbilityPacket.sendToPlayer(player);

                XpAbilities.LOGGER.info("プレイヤー {} がアビリティ {} を習得（コスト: {}xp）",
                        player.getName().getString(), ability.getJapaneseName(), ability.getCost());
            }
        });
    }

    /**
     * プレイヤーの総経験値ポイントを取得
     */
    public static int getPlayerTotalXp(ServerPlayer player) {
        // 現在のレベルまでの累計経験値 + 現在レベルの進行分
        return getXpForLevel(player.experienceLevel)
                + (int)(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /**
     * 指定レベルまでに必要な累計経験値
     */
    private static int getXpForLevel(int level) {
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
     * プレイヤーに経験値ポイントを加算（負の値で減算）
     */
    private static void addPlayerXp(ServerPlayer player, int amount) {
        player.giveExperiencePoints(amount);
    }
}
