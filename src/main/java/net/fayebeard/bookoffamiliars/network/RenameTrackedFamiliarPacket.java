package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.ReleasedFamiliarTracker;
import net.fayebeard.bookoffamiliars.data.TrackedFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public record RenameTrackedFamiliarPacket(UUID familiarUUID, String name) implements CustomPacketPayload {

    public static final Type<RenameTrackedFamiliarPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BookOfFamiliarsMod.MOD_ID, "rename_tracked_familiar"));

    public static final StreamCodec<ByteBuf, RenameTrackedFamiliarPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    RenameTrackedFamiliarPacket::familiarUUID,
                    ByteBufCodecs.STRING_UTF8,
                    RenameTrackedFamiliarPacket::name,
                    RenameTrackedFamiliarPacket::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RenameTrackedFamiliarPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            MinecraftServer server = player.level().getServer();

            ReleasedFamiliarTracker tracker = ReleasedFamiliarTracker.get(server.overworld());
            ReleasedFamiliarTracker.ReleasedEntry entry = tracker.getEntry(packet.familiarUUID());
            if (entry == null || !entry.playerUUID().equals(player.getUUID())) return;

            String name = packet.name().trim();
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

            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
            long currentGameTime = player.level().getGameTime();
            List<TrackedFamiliar> tracked = tracker.getEntriesForPlayer(player.getUUID(), server);
            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(
                    data.getFamiliars(), data.getRecovering(), tracked, currentGameTime));
        });
    }
}
