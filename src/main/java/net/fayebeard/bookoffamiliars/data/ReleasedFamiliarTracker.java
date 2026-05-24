package net.fayebeard.bookoffamiliars.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReleasedFamiliarTracker extends SavedData {

    private static final String DATA_NAME = "bookoffamiliars_released";

    private final Map<UUID, ReleasedEntry> releasedMap = new HashMap<>();

    public record ReleasedEntry(UUID playerUUID, StoredFamiliar snapshot) {}

    public static ReleasedFamiliarTracker get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new Factory<>(ReleasedFamiliarTracker::new,
                        ReleasedFamiliarTracker::load),
                DATA_NAME);
    }

    public void track(UUID entityUUID, UUID playerUUID, StoredFamiliar snapshot) {
        releasedMap.put(entityUUID, new ReleasedEntry(playerUUID, snapshot));
        setDirty();
    }

    public boolean isTracked(UUID entityUUID) {
        return releasedMap.containsKey(entityUUID);
    }

    public ReleasedEntry getEntry(UUID entityUUID) {
        return releasedMap.get(entityUUID);
    }

    public void remove(UUID entityUUID) {
        releasedMap.remove(entityUUID);
        setDirty();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, ReleasedEntry> entry : releasedMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("EntityUUID", entry.getKey());
            entryTag.putUUID("PlayerUUID", entry.getValue().playerUUID());
            StoredFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue().snapshot())
                            .ifSuccess(t -> entryTag.put("Snapshot", t));
            list.add(entryTag);
        }
        compoundTag.put("Released", list);
        return compoundTag;
    }

    public static ReleasedFamiliarTracker load(CompoundTag compoundTag, HolderLookup.Provider registries) {
        ReleasedFamiliarTracker tracker = new ReleasedFamiliarTracker();
        ListTag list = compoundTag.getList("Released", Tag.TAG_COMPOUND);
        for (Tag tag : list) {
            CompoundTag entry = (CompoundTag) tag;
            UUID entityUUID = entry.getUUID("EntityUUID");
            UUID playerUUID = entry.getUUID("PlayerUUID");
            StoredFamiliar.CODEC.parse(NbtOps.INSTANCE, entry.get("Snapshot"))
                            .ifSuccess(snapshot ->
                                    tracker.releasedMap.put(entityUUID, new ReleasedEntry(playerUUID, snapshot)));
        }
        return tracker;
    }
}
