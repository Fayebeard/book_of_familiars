package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.*;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.List;

public record DeleteFamiliarPacket(int index, boolean isRecovering) implements CustomPacketPayload {

    public static final Type<DeleteFamiliarPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bookoffamiliars", "delete_familiar"));

    public static final StreamCodec<ByteBuf, DeleteFamiliarPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    DeleteFamiliarPacket::index,
                    ByteBufCodecs.BOOL,
                    DeleteFamiliarPacket::isRecovering,
                    DeleteFamiliarPacket::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteFamiliarPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;

            if (!holdingBook) return;

            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);

            MinecraftServer server = player.level().getServer();

            if (packet.isRecovering()) {
                if (packet.index() < 0 || packet.index() >= data.getRecovering().size()) return;
                data.removeRecovering(packet.index());
            } else {
                if (packet.index() < 0 || packet.index() >= data.getFamiliars().size()) return;
                StoredFamiliar familiar = data.getFamiliars().get(packet.index());
                CompoundTag nbt = familiar.nbt();
                nbt.read("UUID", UUIDUtil.CODEC).ifPresent(uuid ->
                        ReleasedFamiliarTracker.get(server.overworld()).remove(uuid));
                data.removeFamiliar(packet.index());
            }

            PendingRecoveryData pending = PendingRecoveryData.get(server.overworld());
            if (pending.hasPending(player.getUUID())) {
                int maxFamiliars = Config.MAX_FAMILIARS.get();
                List<RecoveringFamiliar> waiting = pending.drainPending(player.getUUID());
                for (RecoveringFamiliar rf : waiting) {
                    boolean bookFull = data.getFamiliars().size() + data.getRecovering().size() >= maxFamiliars;
                    if (bookFull) {
                        pending.addPending(player.getUUID(), rf);
                    } else {
                        data.addRecovering(rf);
                        player.sendSystemMessage(Component.translatable(
                                "bookoffamiliars.familiar_pending_moved", rf.displayName()).withStyle(style -> style.withColor(0xFFFFAA00)));
                    }
                }
            }

            long currentGameTime = player.level().getGameTime();
            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(
                    data.getFamiliars(), data.getRecovering(), currentGameTime));
        });
    }
}
