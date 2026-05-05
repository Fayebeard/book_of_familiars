package net.fayebeard.bookoffamiliars.util;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class ModUtils {

    public static void playSound(ServerPlayer player, SoundEvent sound) {
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(sound),
                SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                0.25f, 1.0f,
                player.level().getRandom().nextLong()
        ));
    }
}
