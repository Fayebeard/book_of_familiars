package net.fayebeard.bookoffamiliars.item;

import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BookOfFamiliarsMod.MOD_ID);

    public static final DeferredItem<Item> FAMILIAR_BOOK = ITEMS.registerItem("familiar_book", FamiliarBookItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
