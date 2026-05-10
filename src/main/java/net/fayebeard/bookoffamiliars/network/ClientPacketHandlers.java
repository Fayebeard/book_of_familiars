package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.GUI.FamiliarBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandlers {
    public static void handleOpenFamiliarBook(OpenFamiliarBookPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof FamiliarBookScreen existingScreen) {
            existingScreen.refresh(packet.familiars());
        } else {
            mc.setScreen(new FamiliarBookScreen(packet.familiars()));
        }
    }
}
