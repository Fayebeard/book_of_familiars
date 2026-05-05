package net.fayebeard.bookffamiliars.sounds;

import net.fayebeard.bookffamiliars.BookOfFamiliarsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BookOfFamiliarsMod.MOD_ID);

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(BookOfFamiliarsMod.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static final RegistryObject<SoundEvent> FAMILIAR_BOOK_OPEN = register("familiar_book_open");
    public static final RegistryObject<SoundEvent> FAMILIAR_BOOK_CLOSE = register("familiar_book_close");
    public static final RegistryObject<SoundEvent> FAMILIAR_STORE       = register("familiar_store");
    public static final RegistryObject<SoundEvent> FAMILIAR_RELEASE     = register("familiar_release");

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
