package net.fayebeard.bookoffamiliars;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = BookOfFamiliarsMod.MOD_ID, dist = Dist.CLIENT)
public class BookOfFamiliarsModClient {
    public BookOfFamiliarsModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
