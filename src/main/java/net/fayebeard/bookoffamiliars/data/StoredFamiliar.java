package net.fayebeard.bookoffamiliars.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

public record StoredFamiliar(CompoundTag nbt, String entityType, String displayName) {

    public StoredFamiliar {
        nbt = nbt.copy();
    }

    /*public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("EntityNbt", nbt);
        tag.putString("EntityType", entityType);
        tag.putString("DisplayName", displayName);
        return tag;
    }

    public static StoredFamiliar fromNbt(CompoundTag tag) {
        return new StoredFamiliar(
                tag.getCompound("EntityNbt"),
                tag.getString("EntityType"),
                tag.getString("DisplayName")
        );
    }*/

    public static final Codec<StoredFamiliar> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CompoundTag.CODEC.fieldOf("nbt").forGetter(StoredFamiliar::nbt),
                    Codec.STRING.fieldOf("entityType").forGetter(StoredFamiliar::entityType),
                    Codec.STRING.fieldOf("displayName").forGetter(StoredFamiliar::displayName)
            ).apply(instance, StoredFamiliar::new));
}
