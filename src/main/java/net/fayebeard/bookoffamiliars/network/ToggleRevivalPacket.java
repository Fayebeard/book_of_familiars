package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.RecoveringFamiliar;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ToggleRevivalPacket(int index, boolean isRecovering) implements CustomPacketPayload {

    public static final Type<ToggleRevivalPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("bookoffamiliars", "toggle_revival"));

    public static final StreamCodec<ByteBuf, ToggleRevivalPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ToggleRevivalPacket::index,
                    ByteBufCodecs.BOOL,
                    ToggleRevivalPacket::isRecovering,
                    ToggleRevivalPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleRevivalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);

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

            long currentGameTime = player.serverLevel().getGameTime();
            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(
                    data.getFamiliars(), data.getRecovering(), currentGameTime));
        });
    }
}
