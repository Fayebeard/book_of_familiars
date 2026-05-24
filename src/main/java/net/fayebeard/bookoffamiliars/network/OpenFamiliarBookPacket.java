package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.data.RecoveringFamiliar;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

public record OpenFamiliarBookPacket(List<StoredFamiliar> familiars, List<RecoveringFamiliar> recovering, long currentGameTime) {

    public static void encode(OpenFamiliarBookPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.familiars.size());
        for (StoredFamiliar familiar : packet.familiars) {
            StoredFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, familiar)
                    .result()
                    .ifPresent(buf::writeNbt);
        }
        buf.writeInt(packet.recovering.size());
        for (RecoveringFamiliar familiar : packet.recovering) {
            RecoveringFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, familiar)
                    .result()
                    .ifPresent(buf::writeNbt);
        }
        buf.writeLong(packet.currentGameTime);
    }

    public static OpenFamiliarBookPacket decode(FriendlyByteBuf buf) {
        int familiarSize = buf.readInt();
        List<StoredFamiliar> familiarList = new ArrayList<>();
        for (int i = 0; i < familiarSize; i++) {
            CompoundTag tag = buf.readNbt();
            StoredFamiliar.CODEC.parse(NbtOps.INSTANCE, tag)
                    .result()
                    .ifPresent(familiarList::add);
        }

        int recoveringSize = buf.readInt();
        List<RecoveringFamiliar> recoveringList = new ArrayList<>();
        for (int i = 0; i < recoveringSize; i++) {
            CompoundTag tag = buf.readNbt();
            RecoveringFamiliar.CODEC.parse(NbtOps.INSTANCE, tag)
                    .result()
                    .ifPresent(recoveringList::add);
        }
        return new OpenFamiliarBookPacket(familiarList, recoveringList, buf.readLong());
    }

    @SuppressWarnings("unused")
    public static void handle(OpenFamiliarBookPacket packet, CustomPayloadEvent.Context ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPacketHandlers.handleOpenFamiliarBook(packet));
    }
}
