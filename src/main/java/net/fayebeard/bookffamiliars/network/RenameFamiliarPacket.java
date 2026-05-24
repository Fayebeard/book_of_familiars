package net.fayebeard.bookffamiliars.network;

import net.fayebeard.bookffamiliars.data.FamiliarBookData;
import net.fayebeard.bookffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record RenameFamiliarPacket(int index, String name, boolean isRecovering) {

    public static void encode(RenameFamiliarPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.index());
        buf.writeUtf(packet.name());
        buf.writeBoolean(packet.isRecovering());
    }

    public static RenameFamiliarPacket decode(FriendlyByteBuf buf) {
        return new RenameFamiliarPacket(buf.readInt(), buf.readUtf(), buf.readBoolean());
    }

    public static void handle(RenameFamiliarPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            FamiliarBookData data = FamiliarBookData.get(player);
            String name = packet.name.trim();
            int index = packet.index;
            if (name.length() > 50) name = name.substring(0, 50);

            if (packet.isRecovering()) {
                if (index < 0 || index >= data.getRecovering().size()) return;
                if (name.isEmpty()) {
                    name = Component.translatable(data.getRecovering().get(index).entityType()).getString();
                }
                data.renameRecovering(index, name);
            } else {
                if (index < 0 || index >= data.getFamiliars().size()) return;
                if (name.isEmpty()) {
                    name = Component.translatable(data.getFamiliars().get(index).entityType()).getString();
                }
                data.renameFamiliar(index, name);
            }
            FamiliarBookData.save(player, data);

            long currentGameTime = player.serverLevel().getGameTime();
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), currentGameTime)
            );
        });
        ctx.setPacketHandled(true);
    }
}
