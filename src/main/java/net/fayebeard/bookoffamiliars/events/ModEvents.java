package net.fayebeard.bookoffamiliars.events;

import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = BookOfFamiliarsMod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        event.getOriginal().revive();
        FamiliarBookData original = event.getOriginal().getData(ModAttachments.FAMILIAR_DATA);
        event.getEntity().setData(ModAttachments.FAMILIAR_DATA, original.copy());
    }
}
