package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RenameFamiliarPacket(int index, String name, boolean isRecovering) implements CustomPacketPayload {

    public static final Type<RenameFamiliarPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("bookoffamiliars", "rename_familiar"));

    public static final StreamCodec<ByteBuf, RenameFamiliarPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RenameFamiliarPacket::index,
                    ByteBufCodecs.STRING_UTF8,
                    RenameFamiliarPacket::name,
                    ByteBufCodecs.BOOL,
                    RenameFamiliarPacket::isRecovering,
                    RenameFamiliarPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RenameFamiliarPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
            String name = packet.name().trim();
            int index = packet.index();
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
            long currentGameTime = player.serverLevel().getGameTime();
            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), currentGameTime));
        });
    }
}
