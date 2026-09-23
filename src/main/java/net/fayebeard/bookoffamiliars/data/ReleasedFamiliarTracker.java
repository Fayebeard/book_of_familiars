package net.fayebeard.bookoffamiliars.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ReleasedFamiliarTracker extends SavedData {

    public record ReleasedEntry(UUID playerUUID, StoredFamiliar snapshot, BlockPos position, ResourceKey<Level> dimension) {}

    private static final Codec<ReleasedEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .fieldOf("playerUUID").forGetter(ReleasedEntry::playerUUID),
                    StoredFamiliar.CODEC.fieldOf("snapshot").forGetter(ReleasedEntry::snapshot),
                    BlockPos.CODEC.fieldOf("position").forGetter(ReleasedEntry::position),
                    ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(ReleasedEntry::dimension)
            ).apply(instance, ReleasedEntry::new));

    public static final SavedDataType<ReleasedFamiliarTracker> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("bookoffamiliars", "released_familiar_tracker"),
            ReleasedFamiliarTracker::new,
            Codec.unboundedMap(
                    Codec.STRING,
                    ENTRY_CODEC
            ).xmap(
                    map -> {
                        ReleasedFamiliarTracker tracker = new ReleasedFamiliarTracker();
                        map.forEach((key, value) ->
                                tracker.releasedMap.put(UUID.fromString(key), value));
                        return tracker;
                    },
                    tracker -> {
                        Map<String, ReleasedEntry> result = new HashMap<>();
                        tracker.releasedMap.forEach((uuid, entry) ->
                                result.put(uuid.toString(), entry));
                        return result;
                    }
            ),
            DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    private final Map<UUID, ReleasedEntry> releasedMap = new HashMap<>();

    public ReleasedFamiliarTracker() {}

    public static ReleasedFamiliarTracker get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(TYPE);
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

    @SuppressWarnings("unused")
    public boolean renameTracked(UUID entityUUID, String newName, HolderLookup.Provider registryAccess) {
        ReleasedEntry entry = releasedMap.get(entityUUID);
        if (entry == null) return false;
        StoredFamiliar old = entry.snapshot();
        CompoundTag nbt = old.nbt().copy();
        if (newName.isEmpty()) {
            nbt.remove("CustomName");
        } else {
            nbt.putString("CustomName", newName);
        }
        StoredFamiliar updated = new StoredFamiliar(nbt, old.entityType(), newName,
                old.currentHealth(), old.maxHealth(), old.speed(), old.attackDamage(),
                old.hasAttackDamage(), old.itemCount(), old.revival());
        releasedMap.put(entityUUID, new ReleasedEntry(entry.playerUUID(), updated, entry.position(), entry.dimension()));
        setDirty();
        return true;
    }
}
