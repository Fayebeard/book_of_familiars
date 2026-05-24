package net.fayebeard.bookoffamiliars.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReleasedFamiliarTracker extends SavedData {

    public record ReleasedEntry(UUID playerUUID, StoredFamiliar snapshot) {}

    private static final Codec<ReleasedEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .fieldOf("playerUUID").forGetter(ReleasedEntry::playerUUID),
                    StoredFamiliar.CODEC.fieldOf("snapshot").forGetter(ReleasedEntry::snapshot)
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
}
