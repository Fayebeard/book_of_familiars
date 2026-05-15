package net.fayebeard.bookoffamiliars.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

public record StoredFamiliar(CompoundTag nbt, String entityType, String displayName,
                             float currentHealth, float maxHealth, float speed, float attackDamage, boolean hasAttackDamage, int itemCount) {

    public StoredFamiliar {
        nbt = nbt.copy();
    }

    public static final Codec<StoredFamiliar> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CompoundTag.CODEC.fieldOf("nbt").forGetter(StoredFamiliar::nbt),
                    Codec.STRING.fieldOf("entityType").forGetter(StoredFamiliar::entityType),
                    Codec.STRING.fieldOf("displayName").forGetter(StoredFamiliar::displayName),
                    Codec.FLOAT.optionalFieldOf("currentHealth", 0f).forGetter(StoredFamiliar::currentHealth),
                    Codec.FLOAT.optionalFieldOf("maxHealth", 0f).forGetter(StoredFamiliar::maxHealth),
                    Codec.FLOAT.optionalFieldOf("speed", 0f).forGetter(StoredFamiliar::speed),
                    Codec.FLOAT.optionalFieldOf("attackDamage", 0f).forGetter(StoredFamiliar::attackDamage),
                    Codec.BOOL.optionalFieldOf("hasAttackDamage", false).forGetter(StoredFamiliar::hasAttackDamage),
                    Codec.INT.optionalFieldOf("itemCount", -1).forGetter(StoredFamiliar::itemCount)
            ).apply(instance, StoredFamiliar::new));
}
