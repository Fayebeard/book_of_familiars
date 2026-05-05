package net.fayebeard.bookoffamiliars.item;

import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BookOfFamiliarsMod.MOD_ID);

    public static final Supplier<CreativeModeTab> FAMILIAR_BOOK_TAB =
            CREATIVE_MODE_TABS.register("familiar_book_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bookoffamiliars.familiar_book_tab"))
                    .icon(() -> new ItemStack(ModItems.FAMILIAR_BOOK.get()))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.FAMILIAR_BOOK);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
