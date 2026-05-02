package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.GUI.FamiliarBookScreen;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record OpenFamiliarBookPacket(List<StoredFamiliar> familiars) implements CustomPacketPayload {
    public static final Type<OpenFamiliarBookPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("bookoffamiliars", "open_familiar_book"));

    public static final StreamCodec<ByteBuf, OpenFamiliarBookPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public OpenFamiliarBookPacket decode(ByteBuf byteBuf) {
                    int size = byteBuf.readInt();
                    List<StoredFamiliar> list = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(ByteBufCodecs.fromCodec(StoredFamiliar.CODEC).decode(byteBuf));
                    }
                    return new OpenFamiliarBookPacket(list);
                }

                @Override
                public void encode(ByteBuf o, OpenFamiliarBookPacket packet) {
                    o.writeInt(packet.familiars().size());
                    for (StoredFamiliar familiar : packet.familiars()) {
                        ByteBufCodecs.fromCodec(StoredFamiliar.CODEC).encode(o, familiar);
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenFamiliarBookPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof FamiliarBookScreen existingScreen) {
                existingScreen.refresh(packet.familiars());
            } else {
                mc.setScreen(new FamiliarBookScreen(packet.familiars()));
            }
        });
    }
}
