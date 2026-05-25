package net.fayebeard.bookoffamiliars;

import com.mojang.logging.LogUtils;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.item.ModCreativeModeTabs;
import net.fayebeard.bookoffamiliars.item.ModItems;
import net.fayebeard.bookoffamiliars.network.*;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(BookOfFamiliarsMod.MOD_ID)
public class BookOfFamiliarsMod {
    public static final String MOD_ID = "bookoffamiliars";

    public static final Logger LOGGER = LogUtils.getLogger();

    public BookOfFamiliarsMod(IEventBus modEventBus, ModContainer modContainer) {

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

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

        registrar.playToServer(
                SkipRecoveryCooldownPacket.TYPE,
                SkipRecoveryCooldownPacket.STREAM_CODEC,
                SkipRecoveryCooldownPacket::handle
        );

        registrar.playToServer(
                DeleteFamiliarPacket.TYPE,
                DeleteFamiliarPacket.STREAM_CODEC,
                DeleteFamiliarPacket::handle
        );

        registrar.playToServer(
                ToggleRevivalPacket.TYPE,
                ToggleRevivalPacket.STREAM_CODEC,
                ToggleRevivalPacket::handle
        );
    }
}
