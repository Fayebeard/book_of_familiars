package net.fayebeard.bookffamiliars.network;

import net.fayebeard.bookffamiliars.data.RecoveringFamiliar;
import net.fayebeard.bookffamiliars.data.StoredFamiliar;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record OpenFamiliarBookPacket(List<StoredFamiliar> familiars, List<RecoveringFamiliar> recovering, long currentGameTime) {

    public static void encode(OpenFamiliarBookPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.familiars.size());
        for (StoredFamiliar familiar : packet.familiars) {
            StoredFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, familiar)
                    .result()
                    .ifPresent(tag -> buf.writeNbt((CompoundTag) tag));
        }
        buf.writeInt(packet.recovering.size());
        for (RecoveringFamiliar familiar : packet.recovering) {
            RecoveringFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, familiar)
                    .result()
                    .ifPresent(tag -> buf.writeNbt((CompoundTag) tag));
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
            (RecoveringFamiliar.CODEC).parse(NbtOps.INSTANCE, tag)
                    .result()
                    .ifPresent(recoveringList::add);
        }
        return new OpenFamiliarBookPacket(familiarList, recoveringList, buf.readLong());
    }

    public static void handle(OpenFamiliarBookPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPacketHandlers.handleOpenFamiliarBook(packet)));
        ctx.setPacketHandled(true);
    }
}
