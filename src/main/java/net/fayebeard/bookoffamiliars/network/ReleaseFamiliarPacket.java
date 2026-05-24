package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.*;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.fayebeard.bookoffamiliars.util.ModUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public record ReleaseFamiliarPacket(int index) implements CustomPacketPayload {

    public static final Type<ReleaseFamiliarPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bookoffamiliars", "release_familiar"));

    public static final StreamCodec<ByteBuf, ReleaseFamiliarPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ReleaseFamiliarPacket::index,
                    ReleaseFamiliarPacket::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReleaseFamiliarPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
            List<StoredFamiliar> familiars = data.getFamiliars();

            int index = packet.index();
            if (index < 0 || index >= familiars.size()) return;

            StoredFamiliar familiar = familiars.get(index);

            ServerLevel level = player.level();
            Entity entity = EntityType.loadEntityRecursive(familiar.nbt(), level, EntitySpawnReason.LOAD, e -> e);
            if (entity != null) {
                double angle = Math.toRadians(player.getYRot());
                double offsetX = -Math.sin(angle) * 2;
                double offsetZ = Math.cos(angle) * 2;

                double targetX = player.getX() + offsetX;
                double targetY = player.getY();
                double targetZ = player.getZ() + offsetZ;

                entity.setPos(targetX, targetY, targetZ);
                entity.setYRot(player.getYRot());

                if (!level.noCollision(entity)) {
                    entity.setPos(player.getX(), player.getY(), player.getZ());
                    entity.setYRot(player.getYRot());

                    if (!level.noCollision(entity)) {
                        player.sendSystemMessage(Component.translatable("bookoffamiliars.no_room").withStyle(style -> style.withColor(0xFFFF5555)));
                        return;
                    }
                }

                level.addFreshEntity(entity);

                UUID entityUUID = entity.getUUID();
                MinecraftServer server = player.level().getServer();

                ReleasedFamiliarTracker.get(server.overworld()).track(entityUUID, player.getUUID(), familiar);
                level.sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                        50, 0.5, 0.5, 0.5, 0.5);
                data.removeFamiliar(index);
                ModUtils.playSound(player, ModSounds.FAMILIAR_RELEASE.get());

                PendingRecoveryData pending = PendingRecoveryData.get(server.overworld());
                if (pending.hasPending(player.getUUID())) {
                    List<RecoveringFamiliar> waiting = pending.drainPending(player.getUUID());
                    for (RecoveringFamiliar rf : waiting) {
                        if (data.isFull(Config.MAX_FAMILIARS.get())) {
                            pending.addPending(player.getUUID(), rf);
                        } else {
                            data.addRecovering(rf);
                            player.sendSystemMessage(Component.translatable(
                                    "bookoffamiliars.familiar_pending_moved", rf.displayName()).withStyle(style -> style.withColor(0xFFFFAA00)));
                        }
                    }
                }
            }

            long currentGameTime = player.level().getGameTime();
            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), currentGameTime));
        });
    }
}
