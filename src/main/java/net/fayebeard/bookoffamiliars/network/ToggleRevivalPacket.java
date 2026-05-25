package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.RecoveringFamiliar;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

public record ToggleRevivalPacket(int index, boolean isRecovering) {

    public static void encode(ToggleRevivalPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.index());
        buf.writeBoolean(packet.isRecovering());
    }

    public static ToggleRevivalPacket decode (FriendlyByteBuf buf) {
        return new ToggleRevivalPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(ToggleRevivalPacket packet, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;

        boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
        if (!holdingBook) return;

        FamiliarBookData data = FamiliarBookData.get(player);

        if (packet.isRecovering()) {
            if (packet.index() < 0 || packet.index() >= data.getRecovering().size()) return;
            RecoveringFamiliar old = data.getRecovering().get(packet.index());
            data.removeRecovering(packet.index());
            data.addRecovering(new RecoveringFamiliar(
                    old.nbt(), old.entityType(), old.displayName(),
                    old.currentHealth(), old.maxHealth(), old.speed(),
                    old.attackDamage(), old.hasAttackDamage(), old.itemCount(),
                    old.familiarUUID(), old.recoverAt(), !old.revival()));
        } else {
            if (packet.index() < 0 || packet.index() >= data.getFamiliars().size()) return;
            StoredFamiliar old = data.getFamiliars().get(packet.index());
            data.removeFamiliar(packet.index());
            data.addFamiliarAt(packet.index(), new StoredFamiliar(
                    old.nbt(), old.entityType(), old.displayName(),
                    old.currentHealth(), old.maxHealth(), old.speed(),
                    old.attackDamage(), old.hasAttackDamage(), old.itemCount(),
                    !old.revival()));
        }
        FamiliarBookData.save(player, data);

        long currentGameTime = player.serverLevel().getGameTime();
        ModNetwork.CHANNEL.send(
                new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), currentGameTime),
                PacketDistributor.PLAYER.with(player));
    }
}
