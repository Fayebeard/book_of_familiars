package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RenameFamiliarPacket(int index, String name) implements CustomPacketPayload {

    public static final Type<RenameFamiliarPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bookoffamiliars", "rename_familiar"));

    public static final StreamCodec<ByteBuf, RenameFamiliarPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RenameFamiliarPacket::index,
                    ByteBufCodecs.STRING_UTF8,
                    RenameFamiliarPacket::name,
                    RenameFamiliarPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RenameFamiliarPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
            String name = packet.name.trim();
            if (name.length() > 50) name = name.substring(0, 50);

            data.renameFamiliar(packet.index(), name, player.registryAccess());

            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(data.getFamiliars()));
        });
    }
}
