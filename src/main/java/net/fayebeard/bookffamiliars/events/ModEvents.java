package net.fayebeard.bookffamiliars.events;

import net.fayebeard.bookffamiliars.BookOfFamiliarsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BookOfFamiliarsMod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        if (event.getOriginal().getPersistentData().contains("FamiliarData")) {
            CompoundTag tag = event.getOriginal().getPersistentData().getCompound("FamiliarData");
            event.getEntity().getPersistentData().put("FamiliarData", tag);
        }
    }
}
