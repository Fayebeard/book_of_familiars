package net.fayebeard.bookffamiliars.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PendingRecoveryData extends SavedData {

    private static final String DATA_NAME = "bookoffamiliars_pending_recovery";

    private final Map<UUID, List<RecoveringFamiliar>> pendingMap = new HashMap<>();

    public static PendingRecoveryData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                PendingRecoveryData::load,
                PendingRecoveryData::new,
                DATA_NAME);
    }

    public void addPending(UUID playerUUID, RecoveringFamiliar rf) {
        pendingMap.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(rf);
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

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, List<RecoveringFamiliar>> entry : pendingMap.entrySet()) {
            ListTag list = new ListTag();
            for (RecoveringFamiliar rf : entry.getValue()) {
                RecoveringFamiliar.CODEC.encodeStart(NbtOps.INSTANCE, rf)
                        .result()
                        .ifPresent(list::add);
            }
            playersTag.put(entry.getKey().toString(), list);
        }
        compoundTag.put("Pending", playersTag);
        return compoundTag;
    }

    public static PendingRecoveryData load(CompoundTag compoundTag) {
        PendingRecoveryData data = new PendingRecoveryData();
        CompoundTag playersTag = compoundTag.getCompound("Pending");
        for (String key : playersTag.getAllKeys()) {
            UUID playerUUID = UUID.fromString(key);
            ListTag list = playersTag.getList(key, Tag.TAG_COMPOUND);
            List<RecoveringFamiliar> entries = new ArrayList<>();
            for (Tag t : list) {
                RecoveringFamiliar.CODEC.parse(NbtOps.INSTANCE, t)
                        .result()
                        .ifPresent(entries::add);
            }
            data.pendingMap.put(playerUUID, entries);
        }
        return data;
    }
}
