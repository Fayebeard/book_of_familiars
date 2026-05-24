package net.fayebeard.bookffamiliars.network;

import net.fayebeard.bookffamiliars.Config;
import net.fayebeard.bookffamiliars.data.FamiliarBookData;
import net.fayebeard.bookffamiliars.data.RecoveringFamiliar;
import net.fayebeard.bookffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record SkipRecoveryCooldownPacket(int index) {

    public static void encode(SkipRecoveryCooldownPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.index());
    }

    public static SkipRecoveryCooldownPacket decode(FriendlyByteBuf buf) {
        return new SkipRecoveryCooldownPacket(buf.readInt());
    }

    public static void handle(SkipRecoveryCooldownPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            FamiliarBookData data = FamiliarBookData.get(player);
            int index = packet.index();

            if (index < 0 || index >= data.getRecovering().size()) return;

            if (!player.isCreative()) {
                int xpCost = Config.RESURRECTION_XP_COST.get();
                if (player.experienceLevel < xpCost) {
                    player.sendSystemMessage(Component.translatable(
                                "bookoffamiliars.not_enough_xp", xpCost)
                            .withStyle(style -> style.withColor(0xFF5555)));
                    return;
                }
                player.giveExperienceLevels(-xpCost);
            }

            RecoveringFamiliar recoveringFamiliar = data.getRecovering().get(index);
            data.removeRecovering(index);
            data.addFamiliar(recoveringFamiliar.toStoredFamiliar());

            player.sendSystemMessage(Component.translatable("bookoffamiliars.familiar_revived",
                            recoveringFamiliar.displayName())
                    .withStyle(style -> style.withColor(0x55FF55)));

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
