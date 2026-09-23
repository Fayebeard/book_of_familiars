package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.ReleasedFamiliarTracker;
import net.fayebeard.bookoffamiliars.data.TrackedFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

public record ToggleTrackedRevivalPacket(UUID familiarUUID) implements CustomPacketPayload {

    public static final Type<ToggleTrackedRevivalPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BookOfFamiliarsMod.MOD_ID, "toggle_tracked_revival"));

    public static final StreamCodec<ByteBuf, ToggleTrackedRevivalPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    ToggleTrackedRevivalPacket::familiarUUID,
                    ToggleTrackedRevivalPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleTrackedRevivalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            MinecraftServer server = player.level().getServer();

            ReleasedFamiliarTracker tracker = ReleasedFamiliarTracker.get(server.overworld());
            ReleasedFamiliarTracker.ReleasedEntry entry = tracker.getEntry(packet.familiarUUID());
            if (entry == null || !entry.playerUUID().equals(player.getUUID())) return;

            tracker.updateRevival(packet.familiarUUID(), !entry.snapshot().revival());

            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
            long currentGameTime = player.level().getGameTime();
            List<TrackedFamiliar> tracked = tracker.getEntriesForPlayer(player.getUUID(), server);
            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(
                    data.getFamiliars(), data.getRecovering(), tracked, currentGameTime));
        });
    }
}
