package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.*;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ReleaseFamiliarPacket(int index) implements CustomPacketPayload {

    public static final Type<ReleaseFamiliarPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("bookoffamiliars", "release_familiar"));

    public static final StreamCodec<ByteBuf, ReleaseFamiliarPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ReleaseFamiliarPacket::index,
                    ReleaseFamiliarPacket::new
            );


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
        level.sendParticles(ParticleTypes.WITCH, entity.getX(),entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                50, 0.5, 0.5, 0.5, 0.5);
        player.playNotifySound(ModSounds.FAMILIAR_RELEASE.get(), SoundSource.PLAYERS, 0.25f, 1.0f);
        return true;
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
                        player.sendSystemMessage(Component.translatable(
                                    "bookoffamiliars.familiar_pending_moved", rf.displayName()).withStyle(style -> style.withColor(0xFFAA00)));
                    }
                }
            }

            List<TrackedFamiliar> tracked = Config.ENABLE_TRACKING.get()
                    ? ReleasedFamiliarTracker.get(server.overworld()).getEntriesForPlayer(player.getUUID(), server)
                    : List.of();

            long currentGameTime = player.serverLevel().getGameTime();
            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), tracked, currentGameTime));
        });
    }
}
