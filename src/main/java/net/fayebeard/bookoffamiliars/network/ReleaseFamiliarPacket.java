package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.fayebeard.bookoffamiliars.util.ModUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

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

            ServerLevel level = (ServerLevel) player.level();
            Entity entity = EntityType.loadEntityRecursive(familiar.nbt(), level, EntitySpawnReason.LOAD, e -> e);
            if (entity != null) {
                entity.setPos(player.getX(), player.getY(), player.getZ());
                entity.setYRot(player.getYRot());
                level.addFreshEntity(entity);
                data.removeFamiliar(index);
                ModUtils.playSound(player, ModSounds.FAMILIAR_RELEASE.get());
            }

            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(data.getFamiliars()));
        });
    }
}
