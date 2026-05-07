package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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
    public Type<? extends CustomPacketPayload> type() {
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

            Entity entity = EntityType.loadEntityRecursive(familiar.nbt(), player.serverLevel(), e -> e);
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
                        player.sendSystemMessage(Component.translatable("bookoffamiliars.no_room"));
                        return;
                    }
                }
                player.serverLevel().addFreshEntity(entity);
                player.serverLevel().sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                        50, 0.5, 0.5, 0.5, 0.5);
                data.removeFamiliar(index);
                player.playNotifySound(ModSounds.FAMILIAR_RELEASE.get(), SoundSource.PLAYERS, 0.25f, 1.0f);
            }

            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(data.getFamiliars()));
        });
    }
}
