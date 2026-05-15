package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

public record RenameFamiliarPacket(int index, String name) {

    public static void encode(RenameFamiliarPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.index());
        buf.writeUtf(packet.name());
    }

    public static RenameFamiliarPacket decode(FriendlyByteBuf buf) {
        return new RenameFamiliarPacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(RenameFamiliarPacket packet, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;

        boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
        if (!holdingBook) return;

        FamiliarBookData data = FamiliarBookData.get(player);
        int index = packet.index;
        if (index < 0 || index >= data.getFamiliars().size()) return;
        String name = packet.name.trim();
        if (name.length() > 50) name = name.substring(0, 50);
        if (name.isEmpty()) {
            StoredFamiliar familiar = data.getFamiliars().get(index);
            name = Component.translatable(familiar.entityType()).getString();
        }

        data.renameFamiliar(packet.index(), name, player.registryAccess());
        FamiliarBookData.save(player, data);

        ModNetwork.CHANNEL.send(
                new OpenFamiliarBookPacket(data.getFamiliars()),
                PacketDistributor.PLAYER.with(player)
        );
    }
}
