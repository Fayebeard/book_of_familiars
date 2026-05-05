package net.fayebeard.bookoffamiliars.data;


import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
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
}
