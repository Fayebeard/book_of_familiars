package net.fayebeard.bookoffamiliars.data;


import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FamiliarBookData {

    private final List<StoredFamiliar> familiars = new ArrayList<>();

    public static final MapCodec<FamiliarBookData> CODEC = StoredFamiliar.CODEC
            .listOf()
            .xmap(
                    list -> {
                        FamiliarBookData data = new FamiliarBookData();
                        data.familiars.addAll(list);
                        return data;
                    },
                    data -> List.copyOf(data.familiars)
            ).fieldOf("familiars");

    public List<StoredFamiliar> getFamiliars() {
        return Collections.unmodifiableList(familiars);
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

    @SuppressWarnings("unused")
    public void renameFamiliar(int index, String newName, HolderLookup.Provider registryAccess) {
        if (index >= 0 && index < familiars.size()) {
            StoredFamiliar old = familiars.get(index);
            CompoundTag nbt = old.nbt().copy();
            if (newName.isEmpty()) {
                nbt.remove("CustomName");
            } else {
                nbt.putString("CustomName", newName);
            }
            familiars.set(index, new StoredFamiliar(nbt, old.entityType(), newName,
                    old.currentHealth(), old.maxHealth(), old.speed(), old.attackDamage(), old.hasAttackDamage(), old.itemCount()));
        }
    }
}
