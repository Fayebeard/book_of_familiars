package net.fayebeard.bookoffamiliars.sounds;

import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, BookOfFamiliarsMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> FAMILIAR_BOOK_OPEN =
            SOUND_EVENTS.register("familiar_book_open", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(BookOfFamiliarsMod.MOD_ID, "familiar_book_open")
                    )
            );

    public static final DeferredHolder<SoundEvent, SoundEvent> FAMILIAR_BOOK_CLOSE =
            SOUND_EVENTS.register("familiar_book_close", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(BookOfFamiliarsMod.MOD_ID, "familiar_book_close")
                    )
            );

    public static final DeferredHolder<SoundEvent, SoundEvent> FAMILIAR_STORE =
            SOUND_EVENTS.register("familiar_store", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(BookOfFamiliarsMod.MOD_ID, "familiar_store")
                    )
            );

    public static final DeferredHolder<SoundEvent, SoundEvent> FAMILIAR_RELEASE =
            SOUND_EVENTS.register("familiar_release", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(BookOfFamiliarsMod.MOD_ID, "familiar_release")
                    )
            );
}
