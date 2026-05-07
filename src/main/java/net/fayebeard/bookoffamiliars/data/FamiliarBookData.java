package net.fayebeard.bookoffamiliars.data;

import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;

public class FamiliarBookData implements INBTSerializable<CompoundTag> {

    private final List<StoredFamiliar> familiars = new ArrayList<>();

    public List<StoredFamiliar> getFamiliars() {
        return familiars;
    }

    public void addFamiliar(StoredFamiliar familiar) {
        familiars.add(familiar);
    }

    public void removeFamiliar(int index) {
        if (index >= 0 && index < familiars.size()) {
            familiars.remove(index);
        }
    }

    public FamiliarBookData copy() {
        FamiliarBookData copy = new FamiliarBookData();
        for (StoredFamiliar f : familiars) {
            copy.addFamiliar(f);
        }
        return copy;
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
            familiars.set(index, new StoredFamiliar(nbt, old.entityType(), newName));
        }
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (StoredFamiliar f : familiars) {
            DataResult<Tag> result = StoredFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, f);
            result.ifSuccess(list::add);
        }
        tag.put("Familiars", list);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag compoundTag) {
        familiars.clear();
        ListTag list = compoundTag.getList("Familiars", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            StoredFamiliar.CODEC.parse(NbtOps.INSTANCE, list.get(i))
                    .ifSuccess(familiars::add);
        }
    }
}
