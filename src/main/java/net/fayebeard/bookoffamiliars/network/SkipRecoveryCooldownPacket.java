package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.RecoveringFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public record SkipRecoveryCooldownPacket(int index) implements CustomPacketPayload {

    public static final Type<SkipRecoveryCooldownPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bookoffamiliars", "skip_recovery_cooldown"));

    public static final StreamCodec<ByteBuf, SkipRecoveryCooldownPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SkipRecoveryCooldownPacket::index,
                    SkipRecoveryCooldownPacket::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkipRecoveryCooldownPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
            int index = packet.index();

            if (index < 0 || index >= data.getRecovering().size()) return;

            if (!player.isCreative()) {
                int xpCost = Config.RESURRECTION_XP_COST.get();
                if (player.experienceLevel < xpCost) {
                    player.sendSystemMessage(Component.translatable(
                            "bookoffamiliars.not_enough_xp", xpCost).withStyle(style -> style.withColor(0xFFFF5555)));
                    return;
                }
                player.giveExperienceLevels(-xpCost);
            }

            RecoveringFamiliar recoveringFamiliar = data.getRecovering().get(index);
            data.removeRecovering(index);
            data.addFamiliar(recoveringFamiliar.toStoredFamiliar());

            player.sendSystemMessage(Component.translatable("bookoffamiliars.familiar_revived",
                    recoveringFamiliar.displayName()).withStyle(style -> style.withColor(0xFF55FF55)));

            long currentGameTime = player.level().getGameTime();
            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(
                    data.getFamiliars(), data.getRecovering(), currentGameTime));
        });
    }
}
