package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.data.*;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public record DeleteFamiliarPacket(int index, boolean isRecovering) {

    public static void encode(DeleteFamiliarPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.index());
        buf.writeBoolean(packet.isRecovering());
    }

    public static DeleteFamiliarPacket decode(FriendlyByteBuf buf) {
        return new DeleteFamiliarPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(DeleteFamiliarPacket packet, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;

        boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
        if (!holdingBook) return;

        FamiliarBookData data = FamiliarBookData.get(player);

        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (packet.isRecovering()) {
            if (packet.index() < 0 || packet.index() >= data.getRecovering().size()) return;
            data.removeRecovering(packet.index());
        } else {
            if (packet.index() < 0 || packet.index() >= data.getFamiliars().size()) return;
            StoredFamiliar familiar = data.getFamiliars().get(packet.index());
            CompoundTag nbt = familiar.nbt();
            if (nbt.hasUUID("UUID")) {
                ReleasedFamiliarTracker.get(server.overworld()).remove(nbt.getUUID("UUID"));
            }
            data.removeFamiliar(packet.index());
        }
        PendingRecoveryData pending = PendingRecoveryData.get(server.overworld());
        if (pending.hasPending(player.getUUID())) {
            int maxFamiliars = Config.MAX_FAMILIARS.get();
            List<RecoveringFamiliar> waiting = pending.drainPending(player.getUUID());
            for (RecoveringFamiliar rf : waiting) {
                if (data.isFull(maxFamiliars)) {
                    pending.addPending(player.getUUID(), rf);
                } else {
                    data.addRecovering(rf);
                    player.sendSystemMessage(Component.translatable("bookoffamiliars.familiar_pending_moved", rf.displayName())
                                .withStyle(style -> style.withColor(0xFFAA00)));
                }
            }
        }

        FamiliarBookData.save(player, data);

        long currentGameTime = player.level().getGameTime();
        ModNetwork.CHANNEL.send(new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), currentGameTime),
                PacketDistributor.PLAYER.with(player));
    }
}
