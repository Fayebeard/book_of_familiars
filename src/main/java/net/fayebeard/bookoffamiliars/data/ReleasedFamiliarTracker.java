package net.fayebeard.bookoffamiliars.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ReleasedFamiliarTracker extends SavedData {

    private static final String DATA_NAME = "bookoffamiliars_released";

    private final Map<UUID, ReleasedEntry> releasedMap = new HashMap<>();

    public record ReleasedEntry(UUID playerUUID, StoredFamiliar snapshot, BlockPos position, ResourceKey<Level> dimension) {}

    public static ReleasedFamiliarTracker get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new Factory<>(ReleasedFamiliarTracker::new,
                        ReleasedFamiliarTracker::load,
                        DataFixTypes.LEVEL),
                DATA_NAME);
    }

    public void track(UUID entityUUID, UUID playerUUID, StoredFamiliar snapshot, BlockPos position, ResourceKey<Level> dimension) {
        releasedMap.put(entityUUID, new ReleasedEntry(playerUUID, snapshot, position, dimension));
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
            entryTag.putLong("Position", entry.getValue().position.asLong());
            entryTag.putString("Dimension", entry.getValue().dimension().location().toString());
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
            BlockPos position = BlockPos.of(entry.getLong("Position"));
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(entry.getString("Dimension")));
            StoredFamiliar.CODEC.parse(NbtOps.INSTANCE, entry.get("Snapshot"))
                    .ifSuccess(snapshot ->
                            tracker.releasedMap.put(entityUUID, new ReleasedEntry(playerUUID, snapshot, position, dimension)));
        }
        return tracker;
    }

    public List<TrackedFamiliar> getEntriesForPlayer(UUID playerUUID, MinecraftServer server) {
        List<TrackedFamiliar> result = new ArrayList<>();
        for (Map.Entry<UUID, ReleasedEntry> entry : releasedMap.entrySet()) {
            if (!entry.getValue().playerUUID().equals(playerUUID)) continue;

            UUID entityUUID = entry.getKey();
            ReleasedEntry current = entry.getValue();

            for (ServerLevel level : server.getAllLevels()) {
                Entity liveEntity = level.getEntity(entityUUID);
                if (liveEntity != null) {
                    ReleasedEntry updated = new ReleasedEntry(
                            current.playerUUID(), current.snapshot(),
                            liveEntity.blockPosition(), level.dimension());
                    releasedMap.put(entityUUID, updated);
                    current = updated;
                    setDirty();
                    break;
                }
            }
            result.add(new TrackedFamiliar(current.snapshot(), entityUUID, current.position(), current.dimension()));
        }
        return result;
    }

    public boolean updateRevival(UUID entityUUID, boolean revival) {
        ReleasedEntry entry = releasedMap.get(entityUUID);
        if (entry == null) return false;
        StoredFamiliar old = entry.snapshot();
        StoredFamiliar updated = new StoredFamiliar(old.nbt(), old.entityType(), old.displayName(),
                old.currentHealth(), old.maxHealth(), old.speed(), old.attackDamage(),
                old.hasAttackDamage(), old.itemCount(), revival);
        releasedMap.put(entityUUID, new ReleasedEntry(entry.playerUUID(), updated, entry.position(), entry.dimension()));
        setDirty();
        return true;
    }

    public boolean renameTracked(UUID entityUUID, String newName, HolderLookup.Provider registryAccess) {
        ReleasedEntry entry = releasedMap.get(entityUUID);
        if (entry == null) return false;
        StoredFamiliar old = entry.snapshot();
        CompoundTag nbt = old.nbt().copy();
        if (newName.isEmpty()) {
            nbt.remove("CustomName");
        } else {
            nbt.putString("CustomName", Component.Serializer.toJson(
                    Component.literal(newName), registryAccess));
        }
        StoredFamiliar updated = new StoredFamiliar(nbt, old.entityType(), newName,
                old.currentHealth(), old.maxHealth(), old.speed(), old.attackDamage(),
                old.hasAttackDamage(), old.itemCount(), old.revival());
        releasedMap.put(entityUUID, new ReleasedEntry(entry.playerUUID(), updated, entry.position(), entry.dimension()));
        setDirty();
        return true;
    }
}
