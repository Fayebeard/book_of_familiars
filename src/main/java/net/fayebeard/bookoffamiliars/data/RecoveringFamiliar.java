package net.fayebeard.bookoffamiliars.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record RecoveringFamiliar(
        CompoundTag nbt,
        String entityType,
        String displayName,
        float currentHealth,
        float maxHealth,
        float speed,
        float attackDamage,
        boolean hasAttackDamage,
        int itemCount,
        UUID familiarUUID,
        long recoverAt,
        boolean revival) {

    public RecoveringFamiliar {
        nbt = nbt.copy();
    }

    public static final Codec<RecoveringFamiliar> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CompoundTag.CODEC.fieldOf("nbt").forGetter(RecoveringFamiliar::nbt),
                    Codec.STRING.fieldOf("entityType").forGetter(RecoveringFamiliar::entityType),
                    Codec.STRING.fieldOf("displayName").forGetter(RecoveringFamiliar::displayName),
                    Codec.FLOAT.optionalFieldOf("currentHealth", 0f).forGetter(RecoveringFamiliar::currentHealth),
                    Codec.FLOAT.optionalFieldOf("maxHealth", 0f).forGetter(RecoveringFamiliar::maxHealth),
                    Codec.FLOAT.optionalFieldOf("speed", 0f).forGetter(RecoveringFamiliar::speed),
                    Codec.FLOAT.optionalFieldOf("attackDamage", 0f).forGetter(RecoveringFamiliar::attackDamage),
                    Codec.BOOL.optionalFieldOf("hasAttackDamage", false).forGetter(RecoveringFamiliar::hasAttackDamage),
                    Codec.INT.optionalFieldOf("itemCount", -1).forGetter(RecoveringFamiliar::itemCount),
                    Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("familiarUUID").forGetter(RecoveringFamiliar::familiarUUID),
                    Codec.LONG.fieldOf("recoverAt").forGetter(RecoveringFamiliar::recoverAt),
                    Codec.BOOL.fieldOf("revival").forGetter(RecoveringFamiliar::revival)
            ).apply(instance, RecoveringFamiliar::new));

    public static RecoveringFamiliar from(StoredFamiliar familiar, UUID uuid, long recoverAt) {
        return new RecoveringFamiliar(
                familiar.nbt(), familiar.entityType(), familiar.displayName(),
                familiar.currentHealth(), familiar.maxHealth(), familiar.speed(),
                familiar.attackDamage(), familiar.hasAttackDamage(), familiar.itemCount(),
                uuid, recoverAt, familiar.revival()
        );
    }

    public StoredFamiliar toStoredFamiliar() {
        return new StoredFamiliar(nbt, entityType, displayName,
                currentHealth, maxHealth, speed, attackDamage, hasAttackDamage, itemCount, revival);
    }
}
