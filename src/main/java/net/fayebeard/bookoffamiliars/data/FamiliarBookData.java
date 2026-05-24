package net.fayebeard.bookoffamiliars.data;

import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class FamiliarBookData {

    private final List<StoredFamiliar> familiars = new ArrayList<>();
    private final List<RecoveringFamiliar> recovering = new ArrayList<>();

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

    public void renameRecovering(int index, String newName, HolderLookup.Provider registryAccess) {
        if (index >= 0 && index < recovering.size()) {
            RecoveringFamiliar old = recovering.get(index);
            CompoundTag nbt = old.nbt().copy();
            if (newName.isEmpty()) {
                nbt.remove("CustomName");
            } else {
                nbt.putString("CustomName", Component.Serializer.toJson(
                        Component.literal(newName), registryAccess));
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

    @SuppressWarnings("unused")
    public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
        CompoundTag tag = new CompoundTag();
        ListTag familiarList = new ListTag();
        for (StoredFamiliar f : familiars) {
            StoredFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, f).result().ifPresent(familiarList::add);
        }
        tag.put("Familiars", familiarList);

        ListTag recoveringList = new ListTag();
        for (RecoveringFamiliar r : recovering) {
            RecoveringFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, r).result().ifPresent(recoveringList::add);
        }
        tag.put("Recovering", recoveringList);
        return tag;
    }

    @SuppressWarnings("unused")
    public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
        familiars.clear();
        recovering.clear();
        ListTag familiarList = nbt.getList("Familiars", Tag.TAG_COMPOUND);
        for (Tag tag : familiarList) {
            StoredFamiliar.CODEC.parse(NbtOps.INSTANCE, tag).ifSuccess(familiars::add);
        }

        ListTag recoveringList = nbt.getList("Recovering", Tag.TAG_COMPOUND);
        for (Tag tag : recoveringList) {
            RecoveringFamiliar.CODEC.parse(NbtOps.INSTANCE, tag).ifSuccess(recovering::add);
        }
    }
}
