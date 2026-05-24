package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.GUI.FamiliarBookScreen;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.client.Minecraft;

public class ClientPacketHandlers {
    public static void handleOpenFamiliarBook(OpenFamiliarBookPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof FamiliarBookScreen existingScreen) {
            existingScreen.refresh(packet.familiars(), packet.recovering(), packet.currentGameTime());
        } else {
            mc.setScreen(new FamiliarBookScreen(packet.familiars(), packet.recovering(), packet.currentGameTime()));
            if (mc.player != null) {
                mc.player.playSound(ModSounds.FAMILIAR_BOOK_OPEN.get(), 0.25f, 1.0f);
            }

        }
    }
}
