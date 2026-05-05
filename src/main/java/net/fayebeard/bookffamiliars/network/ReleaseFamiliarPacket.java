package net.fayebeard.bookffamiliars.network;

import net.fayebeard.bookffamiliars.capabilities.ModCapabilities;
import net.fayebeard.bookffamiliars.data.StoredFamiliar;
import net.fayebeard.bookffamiliars.item.custom.FamiliarBookItem;
import net.fayebeard.bookffamiliars.sounds.ModSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public class ReleaseFamiliarPacket {

    private final int index;

    public ReleaseFamiliarPacket(int index) {
        this.index = index;
    }

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

            player.getCapability(ModCapabilities.FAMILIAR_DATA).ifPresent(data -> {
                List<StoredFamiliar> familiars = data.getFamiliars();
                if (packet.index < 0 || packet.index >= familiars.size()) return;

                StoredFamiliar familiar = familiars.get(packet.index);
                Entity entity = EntityType.loadEntityRecursive(
                        familiar.nbt(), player.serverLevel(), e -> e);

                if (entity != null) {
                    entity.moveTo(player.getX(), player.getY(), player.getZ(),
                            player.getXRot(), 0);
                    player.serverLevel().addFreshEntity(entity);
                    data.removeFamiliar(packet.index);
                    player.playNotifySound(ModSounds.FAMILIAR_RELEASE.get(),
                            SoundSource.PLAYERS, 0.25f, 1.0f);
                }

                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new OpenFamiliarBookPacket(data.getFamiliars())
                );
            });
        });
        ctx.setPacketHandled(true);
    }
}
