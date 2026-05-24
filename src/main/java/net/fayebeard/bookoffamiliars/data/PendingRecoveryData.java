package net.fayebeard.bookoffamiliars.data;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class PendingRecoveryData extends SavedData {

    public static final SavedDataType<PendingRecoveryData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("bookoffamiliars", "pending_recovery"),
            PendingRecoveryData::new,
            Codec.unboundedMap(
                    Codec.STRING,
                    RecoveringFamiliar.CODEC.listOf()
            ).xmap(
                    map -> {
                        PendingRecoveryData data = new PendingRecoveryData();
                        map.forEach((key, value) ->
                                data.pendingMap.put(UUID.fromString(key), new ArrayList<>(value)));
                        return data;
                    },
                    data -> {
                        Map<String, List<RecoveringFamiliar>> result = new HashMap<>();
                        data.pendingMap.forEach((uuid, list) ->
                                result.put(uuid.toString(), List.copyOf(list)));
                        return result;
                    }
            ),
            DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    private final Map<UUID, List<RecoveringFamiliar>> pendingMap = new HashMap<>();

    public PendingRecoveryData() {}

    public static PendingRecoveryData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public void addPending(UUID playerUUID, RecoveringFamiliar rf) {
        pendingMap.computeIfAbsent(playerUUID, _ -> new ArrayList<>()).add(rf);
        setDirty();
    }

    public List<RecoveringFamiliar> drainPending(UUID playerUUID) {
        List<RecoveringFamiliar> entries = pendingMap.remove(playerUUID);
        if (entries != null) setDirty();
        return entries != null ? entries : Collections.emptyList();
    }

    public boolean hasPending(UUID playerUUID) {
        return pendingMap.containsKey(playerUUID);
    }
}
