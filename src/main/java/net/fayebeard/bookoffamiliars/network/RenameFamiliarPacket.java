package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

public record RenameFamiliarPacket(int index, String name, boolean isRecovering) {

    public static void encode(RenameFamiliarPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.index());
        buf.writeUtf(packet.name());
        buf.writeBoolean(packet.isRecovering());
    }

    public static RenameFamiliarPacket decode(FriendlyByteBuf buf) {
        return new RenameFamiliarPacket(buf.readInt(), buf.readUtf(), buf.readBoolean());
    }

    public static void handle(RenameFamiliarPacket packet, CustomPayloadEvent.Context ctx) {
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
            data.renameRecovering(index, name, player.registryAccess());
        } else {
            if (index < 0 || index >= data.getFamiliars().size()) return;
            if (name.isEmpty()) {
                name = Component.translatable(data.getFamiliars().get(index).entityType()).getString();
            }
            data.renameFamiliar(index, name, player.registryAccess());
        }
        FamiliarBookData.save(player, data);

        long currentGameTime = player.serverLevel().getGameTime();
        ModNetwork.CHANNEL.send(
                new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), currentGameTime),
                PacketDistributor.PLAYER.with(player)
        );
    }
}
