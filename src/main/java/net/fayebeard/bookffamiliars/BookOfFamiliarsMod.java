package net.fayebeard.bookffamiliars;

import com.mojang.logging.LogUtils;
import net.fayebeard.bookffamiliars.events.ModEvents;
import net.fayebeard.bookffamiliars.item.ModCreativeModeTabs;
import net.fayebeard.bookffamiliars.item.ModItems;
import net.fayebeard.bookffamiliars.network.ModNetwork;
import net.fayebeard.bookffamiliars.sounds.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BookOfFamiliarsMod.MOD_ID)
public class BookOfFamiliarsMod {
    public static final String MOD_ID = "bookoffamiliars";

    public static final Logger LOGGER = LogUtils.getLogger();

    public BookOfFamiliarsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModSounds.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(ModEvents.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
