package net.fayebeard.bookffamiliars.item;

import net.fayebeard.bookffamiliars.BookOfFamiliarsMod;
import net.fayebeard.bookffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BookOfFamiliarsMod.MOD_ID);

    public static final RegistryObject<Item> FAMILIAR_BOOK = ITEMS.register("familiar_book",
            () -> new FamiliarBookItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
