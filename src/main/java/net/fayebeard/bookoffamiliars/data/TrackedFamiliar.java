package net.fayebeard.bookoffamiliars.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record TrackedFamiliar(StoredFamiliar snapshot, UUID familiarUUID, BlockPos position, ResourceKey<Level> dimension) {

    public static final Codec<TrackedFamiliar> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StoredFamiliar.CODEC.fieldOf("snapshot").forGetter(TrackedFamiliar::snapshot),
                    Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("familiarUUID").forGetter(TrackedFamiliar::familiarUUID),
                    BlockPos.CODEC.fieldOf("position").forGetter(TrackedFamiliar::position),
                    ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(TrackedFamiliar::dimension)
            ).apply(instance, TrackedFamiliar::new));
}
