package net.fayebeard.bookoffamiliars.data;


import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

public class FamiliarBookData {

    private final List<StoredFamiliar> familiars = new ArrayList<>();
    private final List<RecoveringFamiliar> recovering = new ArrayList<>();

    public static final MapCodec<FamiliarBookData> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    StoredFamiliar.CODEC.listOf()
                            .optionalFieldOf("familiars", List.of())
                            .forGetter(data -> List.copyOf(data.familiars)),
                    RecoveringFamiliar.CODEC.listOf()
                            .optionalFieldOf("recovering", List.of())
                            .forGetter(data -> List.copyOf(data.recovering))
            ).apply(instance, (familiars, recovering) -> {
                FamiliarBookData data = new FamiliarBookData();
                data.familiars.addAll(familiars);
                data.recovering.addAll(recovering);
                return data;
            }));

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

    public List<RecoveringFamiliar> getRecovering() {
        return Collections.unmodifiableList(recovering);
    }

    public void addRecovering(RecoveringFamiliar rf) {
        recovering.add(rf);
        recovering.sort(Comparator.comparingLong(RecoveringFamiliar::recoverAt));
    }

    public void removeRecovering(int index) {
        if (index >= 0 && index < recovering.size()) {
            recovering.remove(index);
        }
    }

    public int promoteReadyFamiliars(long currentGameTime, int maxFamiliars) {
        int count = 0;
        Iterator<RecoveringFamiliar> it = recovering.iterator();
        while (it.hasNext()) {
            RecoveringFamiliar rf = it.next();
            if (currentGameTime < rf.recoverAt()) break;
            if (familiars.size() >= maxFamiliars) break;
            familiars.add(rf.toStoredFamiliar());
            it.remove();
            count++;
        }
        return count;
    }

    public FamiliarBookData copy() {
        FamiliarBookData copy = new FamiliarBookData();
        for (StoredFamiliar f : familiars) copy.addFamiliar(f);
        for (RecoveringFamiliar r : recovering) copy.addRecovering(r);
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

    @SuppressWarnings("unused")
    public void renameRecovering(int index, String newName, HolderLookup.Provider registryAccess) {
        if (index >= 0 && index < recovering.size()) {
            RecoveringFamiliar old = recovering.get(index);
            CompoundTag nbt = old.nbt().copy();
            if (newName.isEmpty()) {
                nbt.remove("CustomName");
            } else {
                nbt.putString("CustomName", newName);
            }
            recovering.set(index, new RecoveringFamiliar(nbt, old.entityType(), newName,
                    old.currentHealth(), old.maxHealth(), old.speed(),
                    old.attackDamage(), old.hasAttackDamage(), old.itemCount(),
                    old.familiarUUID(), old.recoverAt()));
        }
    }

    public boolean isFull(int maxFamiliars) {
        return familiars.size() + recovering.size() >= maxFamiliars;
    }

    public List<String> removeUnresolvableEntities(String playerName) {
        List<String> removed = new ArrayList<>();
        Iterator<StoredFamiliar> it = familiars.iterator();
        while (it.hasNext()) {
            StoredFamiliar f = it.next();
            boolean exists = BuiltInRegistries.ENTITY_TYPE.stream()
                    .anyMatch(e -> e.getDescriptionId().equals(f.entityType()));
            if (!exists) {
                removed.add(f.displayName());
                BookOfFamiliarsMod.LOGGER.debug(
                        "Removed unresolvable familiar '{}' (type: {}) from {}'s book.",
                        f.displayName(), f.entityType(), playerName);
                it.remove();
            }
        }
        Iterator<RecoveringFamiliar> rit = recovering.iterator();
        while (rit.hasNext()) {
            RecoveringFamiliar r = rit.next();
            boolean exists = BuiltInRegistries.ENTITY_TYPE.stream()
                    .anyMatch(e -> e.getDescriptionId().equals(r.entityType()));
            if (!exists) {
                removed.add(r.displayName());
                BookOfFamiliarsMod.LOGGER.debug(
                        "Removed unresolvable recovering familiar '{}' (type: {}) from {}'s book.",
                        r.displayName(), r.entityType(), playerName);
                rit.remove();
            }
        }
        return removed;
    }
}
