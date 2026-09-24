package net.fayebeard.bookffamiliars.network;

import net.fayebeard.bookffamiliars.data.FamiliarBookData;
import net.fayebeard.bookffamiliars.data.ReleasedFamiliarTracker;
import net.fayebeard.bookffamiliars.data.TrackedFamiliar;
import net.fayebeard.bookffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record ToggleTrackedRevivalPacket(UUID familiarUUID) {

    public static void encode(ToggleTrackedRevivalPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.familiarUUID());
    }

    public static ToggleTrackedRevivalPacket decode(FriendlyByteBuf buf) {
        return new ToggleTrackedRevivalPacket(buf.readUUID());
    }

    public static void handle(ToggleTrackedRevivalPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;

            if (!holdingBook) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;

            ReleasedFamiliarTracker tracker = ReleasedFamiliarTracker.get(server.overworld());
            ReleasedFamiliarTracker.ReleasedEntry entry = tracker.getEntry(packet.familiarUUID());
            if (entry == null || !entry.playerUUID().equals(player.getUUID())) return;

            tracker.updateRevival(packet.familiarUUID(), !entry.snapshot().revival());

            FamiliarBookData data = FamiliarBookData.get(player);
            long currentGameTime = player.level().getGameTime();
            List<TrackedFamiliar> tracked = tracker.getEntriesForPlayer(player.getUUID(), server);
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), tracked, currentGameTime));
        });
    }
}
