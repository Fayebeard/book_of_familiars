package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.ReleasedFamiliarTracker;
import net.fayebeard.bookoffamiliars.data.TrackedFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public record RenameTrackedFamiliarPacket(UUID familiarUUID, String name) {

    public static void encode(RenameTrackedFamiliarPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.familiarUUID());
        buf.writeUtf(packet.name());
    }

    public static RenameTrackedFamiliarPacket decode(FriendlyByteBuf buf) {
        return new RenameTrackedFamiliarPacket(buf.readUUID(), buf.readUtf());
    }

    public static void handle(RenameTrackedFamiliarPacket packet, CustomPayloadEvent.Context ctx) {
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

        String name = packet.name.trim();
        if (name.length() > 50) name = name.substring(0, 50);
        if (name.isEmpty()) {
            name = Component.translatable(entry.snapshot().entityType()).getString();
        }

        tracker.renameTracked(packet.familiarUUID(), name, player.registryAccess());

        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(packet.familiarUUID());
            if (entity != null) {
                if (name.isEmpty()) {
                    entity.setCustomName(null);
                } else {
                    entity.setCustomName(Component.literal(name));
                }
                entity.setCustomNameVisible(true);
                break;
            }
        }

        FamiliarBookData data = FamiliarBookData.get(player);
        long currentGameTime = player.serverLevel().getGameTime();
        List<TrackedFamiliar> tracked = tracker.getEntriesForPlayer(player.getUUID(), server);
        ModNetwork.CHANNEL.send(
                new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), tracked, currentGameTime),
                PacketDistributor.PLAYER.with(player)
        );
    }
}
