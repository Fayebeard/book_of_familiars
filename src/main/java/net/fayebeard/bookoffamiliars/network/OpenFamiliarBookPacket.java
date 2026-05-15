package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

public record OpenFamiliarBookPacket(List<StoredFamiliar> familiars) {

    public static void encode(OpenFamiliarBookPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.familiars.size());
        for (StoredFamiliar familiar : packet.familiars) {
            StoredFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, familiar)
                    .result()
                    .ifPresent(buf::writeNbt);
        }
    }

    public static OpenFamiliarBookPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<StoredFamiliar> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            CompoundTag tag = buf.readNbt();
            StoredFamiliar.CODEC.parse(NbtOps.INSTANCE, tag)
                    .result()
                    .ifPresent(list::add);
        }
        return new OpenFamiliarBookPacket(list);
    }

    @SuppressWarnings("unused")
    public static void handle(OpenFamiliarBookPacket packet, CustomPayloadEvent.Context ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPacketHandlers.handleOpenFamiliarBook(packet));
    }
}
