package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.GUI.FamiliarBookScreen;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

public record OpenFamiliarBookPacket(List<StoredFamiliar> familiars) {

    public static void encode(OpenFamiliarBookPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.familiars.size());
        for (StoredFamiliar familiar : packet.familiars) {
            StoredFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, familiar)
                    .result()
                    .ifPresent(tag -> buf.writeNbt((CompoundTag) tag));
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

    public static void handle(OpenFamiliarBookPacket packet, CustomPayloadEvent.Context ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof FamiliarBookScreen existingScreen) {
            existingScreen.refresh(packet.familiars());
        } else {
            mc.setScreen(new FamiliarBookScreen(packet.familiars()));
        }
    }
}
