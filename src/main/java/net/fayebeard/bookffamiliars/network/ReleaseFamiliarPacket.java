package net.fayebeard.bookffamiliars.network;

import net.fayebeard.bookffamiliars.Config;
import net.fayebeard.bookffamiliars.data.*;
import net.fayebeard.bookffamiliars.item.custom.FamiliarBookItem;
import net.fayebeard.bookffamiliars.sounds.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public record ReleaseFamiliarPacket(int index) {

    public static void encode(ReleaseFamiliarPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.index);
    }

    public static ReleaseFamiliarPacket decode(FriendlyByteBuf buf) {
        return new ReleaseFamiliarPacket(buf.readInt());
    }

    public static Entity loadFamiliarEntity(ServerPlayer player, StoredFamiliar familiar) {
        return EntityType.loadEntityRecursive(familiar.nbt(), player.serverLevel(), e -> e);
    }

    public static boolean placeFamiliar(ServerPlayer player, Entity entity) {
        ServerLevel level = player.serverLevel();

        double angle = Math.toRadians(player.getYRot());
        entity.moveTo(player.getX() - Math.sin(angle) * 2, player.getY(),
                player.getZ() + Math.cos(angle) * 2, player.getYRot(), 0);

        if (!level.noCollision(entity)) {
            entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
            if (!level.noCollision(entity)) {
                player.displayClientMessage(Component.translatable("bookoffamiliars.no_room")
                        .withStyle(style -> style.withColor(0xFF5555)), true);
                return false;
            }
        }
        if (!level.addFreshEntity(entity)) {
            player.displayClientMessage(Component.translatable("bookoffamiliars.release_failed")
                    .withStyle(style -> style.withColor(0xFF5555)), true);
            return false;
        }
        level.sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                50, 0.5, 0.5, 0.5, 0.5);
        player.playNotifySound(ModSounds.FAMILIAR_RELEASE.get(), SoundSource.PLAYERS, 0.25f, 1.0f);
        return true;
    }

    public static void handle(ReleaseFamiliarPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            FamiliarBookData data = FamiliarBookData.get(player);
            List<StoredFamiliar> familiars = data.getFamiliars();
            if (packet.index < 0 || packet.index >= familiars.size()) return;

            StoredFamiliar familiar = familiars.get(packet.index);

            Entity entity = loadFamiliarEntity(player, familiar);
            if (entity == null) return;
            if (!placeFamiliar(player, entity)) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;
            ReleasedFamiliarTracker.get(server.overworld()).track(entity.getUUID(), player.getUUID(), familiar,
                    entity.blockPosition(), player.serverLevel().dimension());
            data.removeFamiliar(packet.index);

            PendingRecoveryData pending = PendingRecoveryData.get(server.overworld());
            if (pending.hasPending(player.getUUID())) {
                List<RecoveringFamiliar> waiting = pending.drainPending(player.getUUID());
                for (RecoveringFamiliar rf : waiting) {
                    if (data.isFull(Config.MAX_FAMILIARS.get())) {
                        pending.addPending(player.getUUID(), rf);
                    } else {
                        data.addRecovering(rf);
                        player.sendSystemMessage(Component.translatable("bookoffamiliars.familiar_pending_moved", rf.displayName())
                                .withStyle(style -> style.withColor(0xFFAA00)));
                    }
                }
            }
            FamiliarBookData.save(player, data);

            List<TrackedFamiliar> tracked = Config.ENABLE_TRACKING.get()
                    ? ReleasedFamiliarTracker.get(server.overworld()).getEntriesForPlayer(player.getUUID(), server)
                    : List.of();
            long currentGameTime = player.serverLevel().getGameTime();
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), tracked, currentGameTime)
            );
        });
        ctx.setPacketHandled(true);
    }
}
