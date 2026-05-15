package net.fayebeard.bookoffamiliars.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FamiliarBookData {

    private final List<StoredFamiliar> familiars = new ArrayList<>();

    public static FamiliarBookData get(Player player) {
        FamiliarBookData data = new FamiliarBookData();
        if (player.getPersistentData().contains("FamiliarData")) {
            data.deserializeNBT(player.level().registryAccess(),
                    player.getPersistentData().getCompound("FamiliarData"));
        }
        return data;
    }

    public static void save(Player player, FamiliarBookData data) {
        player.getPersistentData().put("FamiliarData",
                data.serializeNBT(player.level().registryAccess()));
    }

    public List<StoredFamiliar> getFamiliars() {
        return Collections.unmodifiableList(familiars);
    }

    public void addFamiliar(StoredFamiliar familiar) {
        familiars.add(familiar);
    }

    public void removeFamiliar(int index) {
        if (index >= 0 && index < familiars.size()) familiars.remove(index);
    }

    public void renameFamiliar(int index, String newName, HolderLookup.Provider registryAccess) {
        if (index >= 0 && index < familiars.size()) {
            StoredFamiliar old = familiars.get(index);
            CompoundTag nbt = old.nbt().copy();
            if (newName.isEmpty()) {
                nbt.remove("CustomName");
            } else {
                nbt.putString("CustomName", Component.Serializer.toJson(
                        Component.literal(newName), registryAccess));
            }
            familiars.set(index, new StoredFamiliar(nbt, old.entityType(), newName,
                    old.currentHealth(), old.maxHealth(), old.speed(), old.attackDamage(), old.hasAttackDamage(), old.itemCount()));
        }
    }

    @SuppressWarnings("unused")
    public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (StoredFamiliar f : familiars) {
            StoredFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, f)
                    .result()
                    .ifPresent(list::add);
        }
        tag.put("Familiars", list);
        return tag;
    }

    @SuppressWarnings("unused")
    public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
        familiars.clear();
        ListTag list = nbt.getList("Familiars", Tag.TAG_COMPOUND);
        for (Tag tag : list) {
            StoredFamiliar.CODEC.parse(NbtOps.INSTANCE, tag)
                    .result()
                    .ifPresent(familiars::add);
        }
    }
}
