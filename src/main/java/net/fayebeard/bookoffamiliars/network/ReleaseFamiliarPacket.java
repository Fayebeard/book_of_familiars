package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public record ReleaseFamiliarPacket(int index) {

    public static void encode(ReleaseFamiliarPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.index);
    }

    public static ReleaseFamiliarPacket decode(FriendlyByteBuf buf) {
        return new ReleaseFamiliarPacket(buf.readInt());
    }

    public static void handle(ReleaseFamiliarPacket packet, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;

        boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
        if (!holdingBook) return;

        FamiliarBookData data = FamiliarBookData.get(player);
        List<StoredFamiliar> familiars = data.getFamiliars();
        if (packet.index() < 0 || packet.index() >= familiars.size()) return;

        StoredFamiliar familiar = familiars.get(packet.index());
        Entity entity = EntityType.loadEntityRecursive(
                familiar.nbt(), player.serverLevel(), e -> e);

        if (entity != null) {
            entity.moveTo(player.getX(), player.getY(), player.getZ(),
                    player.getXRot(), 0);
            player.serverLevel().addFreshEntity(entity);
            data.removeFamiliar(packet.index());
            FamiliarBookData.save(player, data);
            player.playNotifySound(ModSounds.FAMILIAR_RELEASE.get(),
                    SoundSource.PLAYERS, 0.25f, 1.0f);
        }

        ModNetwork.CHANNEL.send(
                new OpenFamiliarBookPacket(data.getFamiliars()),
                PacketDistributor.PLAYER.with(player)
        );
    }
}
