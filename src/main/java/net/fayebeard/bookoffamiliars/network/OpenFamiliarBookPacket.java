package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
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

public record OpenFamiliarBookPacket(List<StoredFamiliar> familiars) implements CustomPacketPayload {
    public static final Type<OpenFamiliarBookPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bookoffamiliars", "open_familiar_book"));

    public static final StreamCodec<ByteBuf, OpenFamiliarBookPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public @NonNull OpenFamiliarBookPacket decode(ByteBuf input) {
                    int size = input.readInt();
                    List<StoredFamiliar> list = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(ByteBufCodecs.fromCodec(StoredFamiliar.CODEC).decode(input));
                    }
                    return new OpenFamiliarBookPacket(list);
                }

                @Override
                public void encode(ByteBuf output, OpenFamiliarBookPacket value) {
                    output.writeInt(value.familiars().size());
                    for (StoredFamiliar familiar : value.familiars()) {
                        ByteBufCodecs.fromCodec(StoredFamiliar.CODEC).encode(output, familiar);
                    }
                }
            };

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
