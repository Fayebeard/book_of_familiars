package net.fayebeard.bookffamiliars.capabilities;

import net.fayebeard.bookffamiliars.data.FamiliarBookData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FamiliarBookDataProvider implements ICapabilitySerializable<CompoundTag> {

    private final FamiliarBookData data = new FamiliarBookData();

    private final LazyOptional<FamiliarBookData> instance =
            LazyOptional.of(FamiliarBookData::new);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.FAMILIAR_DATA) {
            return instance.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }

    public void invalidate() {
        instance.invalidate();
    }
}
