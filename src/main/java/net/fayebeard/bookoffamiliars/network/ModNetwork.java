package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

public class ModNetwork {

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(BookOfFamiliarsMod.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(OpenFamiliarBookPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenFamiliarBookPacket::encode)
                .decoder(OpenFamiliarBookPacket::decode)
                .consumerMainThread(OpenFamiliarBookPacket::handle)
                .add();

        CHANNEL.messageBuilder(ReleaseFamiliarPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReleaseFamiliarPacket::encode)
                .decoder(ReleaseFamiliarPacket::decode)
                .consumerMainThread(ReleaseFamiliarPacket::handle)
                .add();

        CHANNEL.messageBuilder(RenameFamiliarPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RenameFamiliarPacket::encode)
                .decoder(RenameFamiliarPacket::decode)
                .consumerMainThread(RenameFamiliarPacket::handle)
                .add();

        CHANNEL.messageBuilder(DeleteFamiliarPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteFamiliarPacket::encode)
                .decoder(DeleteFamiliarPacket::decode)
                .consumerMainThread(DeleteFamiliarPacket::handle)
                .add();

        CHANNEL.messageBuilder(SkipRecoveryCooldownPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkipRecoveryCooldownPacket::encode)
                .decoder(SkipRecoveryCooldownPacket::decode)
                .consumerMainThread(SkipRecoveryCooldownPacket::handle)
                .add();

        CHANNEL.messageBuilder(ToggleRevivalPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleRevivalPacket::encode)
                .decoder(ToggleRevivalPacket::decode)
                .consumerMainThread(ToggleRevivalPacket::handle)
                .add();

        CHANNEL.messageBuilder(DeleteTrackedFamiliarPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteTrackedFamiliarPacket::encode)
                .decoder(DeleteTrackedFamiliarPacket::decode)
                .consumerMainThread(DeleteTrackedFamiliarPacket::handle)
                .add();

        CHANNEL.messageBuilder(RecallFamiliarPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RecallFamiliarPacket::encode)
                .decoder(RecallFamiliarPacket::decode)
                .consumerMainThread(RecallFamiliarPacket::handle)
                .add();

        CHANNEL.messageBuilder(RenameTrackedFamiliarPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RenameTrackedFamiliarPacket::encode)
                .decoder(RenameTrackedFamiliarPacket::decode)
                .consumerMainThread(RenameTrackedFamiliarPacket::handle)
                .add();

        CHANNEL.messageBuilder(ToggleTrackedRevivalPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleTrackedRevivalPacket::encode)
                .decoder(ToggleTrackedRevivalPacket::decode)
                .consumerMainThread(ToggleTrackedRevivalPacket::handle)
                .add();
    }
}
