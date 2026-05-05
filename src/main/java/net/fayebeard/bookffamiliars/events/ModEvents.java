package net.fayebeard.bookffamiliars.events;

import net.fayebeard.bookffamiliars.BookOfFamiliarsMod;
import net.fayebeard.bookffamiliars.capabilities.FamiliarBookDataProvider;
import net.fayebeard.bookffamiliars.capabilities.ModCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BookOfFamiliarsMod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            FamiliarBookDataProvider provider = new FamiliarBookDataProvider();
            event.addCapability(
                    new ResourceLocation(BookOfFamiliarsMod.MOD_ID, "familiar_data"),
                    provider
            );
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        event.getOriginal().revive();

        event.getOriginal().getCapability(ModCapabilities.FAMILIAR_DATA).ifPresent(original -> {
            event.getEntity().getCapability(ModCapabilities.FAMILIAR_DATA)
                    .ifPresent(copy -> {
                        CompoundTag tag = original.serializeNBT();
                        copy.deserializeNBT(tag);
                    });
        });
    }
}
