package net.fayebeard.bookoffamiliars;

import com.mojang.logging.LogUtils;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.item.ModCreativeModeTabs;
import net.fayebeard.bookoffamiliars.item.ModItems;
import net.fayebeard.bookoffamiliars.network.OpenFamiliarBookPacket;
import net.fayebeard.bookoffamiliars.network.ReleaseFamiliarPacket;
import net.fayebeard.bookoffamiliars.network.RenameFamiliarPacket;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BookOfFamiliarsMod.MOD_ID)
public class BookOfFamiliarsMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "bookoffamiliars";
    // Directly reference a slf4j logger
    @SuppressWarnings("unused")
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public BookOfFamiliarsMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::registerPayloads);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
          OpenFamiliarBookPacket.TYPE,
          OpenFamiliarBookPacket.STREAM_CODEC,
          OpenFamiliarBookPacket::handle
        );

        registrar.playToServer(
          ReleaseFamiliarPacket.TYPE,
          ReleaseFamiliarPacket.STREAM_CODEC,
          ReleaseFamiliarPacket::handle
        );

        registrar.playToServer(
                RenameFamiliarPacket.TYPE,
                RenameFamiliarPacket.STREAM_CODEC,
                RenameFamiliarPacket::handle
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
}
