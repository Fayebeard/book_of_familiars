package net.fayebeard.bookffamiliars.capabilities;

import net.fayebeard.bookffamiliars.data.FamiliarBookData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class ModCapabilities {

    public static final Capability<FamiliarBookData> FAMILIAR_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(FamiliarBookData.class);
    }
}
