package net.fayebeard.bookoffamiliars.events;

import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.PendingRecoveryData;
import net.fayebeard.bookoffamiliars.data.RecoveringFamiliar;
import net.fayebeard.bookoffamiliars.data.ReleasedFamiliarTracker;
import net.fayebeard.bookoffamiliars.network.OpenFamiliarBookPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = BookOfFamiliarsMod.MOD_ID)
public class ModEvents {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        event.getOriginal().revive();
        FamiliarBookData original = event.getOriginal().getData(ModAttachments.FAMILIAR_DATA);
        event.getEntity().setData(ModAttachments.FAMILIAR_DATA, original.copy());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.level().getServer();

        if (Config.REMOVE_INVALID_FAMILIARS.get()) {
            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
            List<String> removed = data.removeUnresolvableEntities(
                    player.getName().getString());
            if (!removed.isEmpty()) {
                for (String name : removed) {
                    player.sendSystemMessage(Component.translatable(
                                    "bookoffamiliars.familiar_removed_missing_mod", name)
                            .withStyle(style -> style.withColor(0xFFFF5555)));
                }
            }
        }

        PendingRecoveryData pending = PendingRecoveryData.get(server.overworld());
        if (!pending.hasPending(player.getUUID())) return;

        List<RecoveringFamiliar> entries = pending.drainPending(player.getUUID());
        FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);

        for (RecoveringFamiliar rf : entries) {
            if (data.isFull(Config.MAX_FAMILIARS.get())) {
                pending.addPending(player.getUUID(), rf);
                player.sendSystemMessage(Component.translatable(
                        "bookoffamiliars.familiar_recovering_full", rf.displayName()).withStyle(style -> style.withColor(0xFFFFAA00)));
            } else {
                data.addRecovering(rf);
                player.sendSystemMessage(Component.translatable(
                        "bookoffamiliars.familiar_recovering", rf.displayName()).withStyle(style -> style.withColor(0xFFFFAA00)));
            }
        }
    }

    @SubscribeEvent
    public static void onFamiliarDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!Config.ENABLE_RESURRECTION.get()) return;

        MinecraftServer server = event.getEntity().level().getServer();
        if (server == null) return;

        UUID entityUUID = event.getEntity().getUUID();
        ReleasedFamiliarTracker tracker = ReleasedFamiliarTracker.get(server.overworld());

        if (!tracker.isTracked(entityUUID)) return;

        ReleasedFamiliarTracker.ReleasedEntry entry = tracker.getEntry(entityUUID);
        tracker.remove(entityUUID);

        if (!entry.snapshot().revival()) return;

        long cooldownTicks = (long) Config.RESURRECTION_COOLDOWN_MINUTES.get() * 60L * 20L;
        long recoverAt = event.getEntity().level().getGameTime() + cooldownTicks;

        RecoveringFamiliar rf = RecoveringFamiliar.from(entry.snapshot(), entityUUID, recoverAt);

        ServerPlayer player = server.getPlayerList().getPlayer(entry.playerUUID());
        if (player != null) {
            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);

            if (data.isFull(Config.MAX_FAMILIARS.get())) {
                PendingRecoveryData.get(server.overworld()).addPending(entry.playerUUID(), rf);
                player.sendSystemMessage(Component.translatable(
                        "bookoffamiliars.familiar_recovering_full", entry.snapshot().displayName()).withStyle(style -> style.withColor(0xFFFFAA00)));
            } else {
                data.addRecovering(rf);
                player.sendSystemMessage(Component.translatable(
                        "bookoffamiliars.familiar_recovering", entry.snapshot().displayName()).withStyle(style -> style.withColor(0xFFFFAA00)));
            }
        } else {
            PendingRecoveryData.get(server.overworld()).addPending(entry.playerUUID(), rf);
            BookOfFamiliarsMod.LOGGER.debug(
                    "Familiar {} died while owner {} was offline. Stored in pending recovery.",
                    entityUUID, entry.playerUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < 100) return;
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        PendingRecoveryData pending = PendingRecoveryData.get(server.overworld());
        long currentGameTime = server.overworld().getGameTime();
        int maxFamiliars = Config.MAX_FAMILIARS.get();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
            boolean changed = false;
            int promoted = data.promoteReadyFamiliars(currentGameTime, maxFamiliars);

            if (promoted > 0) {
                changed = true;
                player.sendSystemMessage(Component.translatable("bookoffamiliars.familiar_returned", promoted).withStyle(style -> style.withColor(0xFF55FF55)));
            }

            if (pending.hasPending(player.getUUID())) {
                List<RecoveringFamiliar> waiting = pending.drainPending(player.getUUID());
                for (RecoveringFamiliar rf : waiting) {
                    if (data.isFull(maxFamiliars)) {
                        pending.addPending(player.getUUID(), rf);
                    } else {
                        data.addRecovering(rf);
                        changed = true;
                        player.sendSystemMessage(Component.translatable(
                                "bookoffamiliars.familiar_recovering", rf.displayName()).withStyle(style -> style.withColor(0xFFFFAA00)));
                    }
                }
            }

            if (changed) {
                PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(
                        data.getFamiliars(), data.getRecovering(), currentGameTime));
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        tickCounter = 0;
    }
}
