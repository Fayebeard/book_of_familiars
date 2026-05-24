package net.fayebeard.bookffamiliars.network;

import net.fayebeard.bookffamiliars.Config;
import net.fayebeard.bookffamiliars.data.*;
import net.fayebeard.bookffamiliars.item.custom.FamiliarBookItem;
import net.fayebeard.bookffamiliars.sounds.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record ReleaseFamiliarPacket(int index) {

    public static void encode(ReleaseFamiliarPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.index);
    }

    public static ReleaseFamiliarPacket decode(FriendlyByteBuf buf) {
        return new ReleaseFamiliarPacket(buf.readInt());
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
            Entity entity = EntityType.loadEntityRecursive(
                    familiar.nbt(), player.serverLevel(), e -> e);

            if (entity != null) {
                double angle = Math.toRadians(player.getYRot());
                double offsetX = -Math.sin(angle) * 2;
                double offsetZ = Math.cos(angle) * 2;

                double targetX = player.getX() + offsetX;
                double targetY = player.getY();
                double targetZ = player.getZ() + offsetZ;

                entity.moveTo(targetX, targetY, targetZ, player.getYRot(), 0);

                if (!player.serverLevel().noCollision(entity)) {
                    entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);

                    if (!player.serverLevel().noCollision(entity)) {
                        player.sendSystemMessage(Component.translatable("bookoffamiliars.no_room").withStyle(style -> style.withColor(0xFF5555)));
                        return;
                    }
                }
                player.serverLevel().addFreshEntity(entity);

                UUID entityUUID = entity.getUUID();
                MinecraftServer server = player.getServer();
                if (server == null) return;
                ReleasedFamiliarTracker.get(server.overworld()).track(entityUUID, player.getUUID(), familiar);
                player.serverLevel().sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                        50, 0.5, 0.5, 0.5, 0.5);
                data.removeFamiliar(packet.index);
                player.playNotifySound(ModSounds.FAMILIAR_RELEASE.get(), SoundSource.PLAYERS, 0.25f, 1.0f);

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

                long currentGameTime = player.serverLevel().getGameTime();
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), currentGameTime)
                );
            }
        });
        ctx.setPacketHandled(true);
    }
}
