package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.data.RecoveringFamiliar;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public record OpenFamiliarBookPacket(List<StoredFamiliar> familiars, List<RecoveringFamiliar> recovering, long currentGameTime) implements CustomPacketPayload {

    public static final Type<OpenFamiliarBookPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bookoffamiliars", "open_familiar_book"));

    public static final StreamCodec<ByteBuf, OpenFamiliarBookPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.fromCodec(StoredFamiliar.CODEC)),
                    OpenFamiliarBookPacket::familiars,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.fromCodec(RecoveringFamiliar.CODEC)),
                    OpenFamiliarBookPacket::recovering,
                    ByteBufCodecs.VAR_LONG,
                    OpenFamiliarBookPacket::currentGameTime,
                    OpenFamiliarBookPacket::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenFamiliarBookPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.getDist().isClient()) {
                ClientPacketHandlers.handleOpenFamiliarBook(packet);
            }
        });
    }
}
