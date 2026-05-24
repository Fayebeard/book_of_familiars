package net.fayebeard.bookoffamiliars.events;

import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.PendingRecoveryData;
import net.fayebeard.bookoffamiliars.data.RecoveringFamiliar;
import net.fayebeard.bookoffamiliars.data.ReleasedFamiliarTracker;
import net.fayebeard.bookoffamiliars.network.ModNetwork;
import net.fayebeard.bookoffamiliars.network.OpenFamiliarBookPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BookOfFamiliarsMod.MOD_ID)
public class ModEvents {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (event.getOriginal().getPersistentData().contains("FamiliarData")) {
            CompoundTag tag = event.getOriginal().getPersistentData().getCompound("FamiliarData");
            event.getEntity().getPersistentData().put("FamiliarData", tag);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (Config.REMOVE_INVALID_FAMILIARS.get()) {
            FamiliarBookData data = FamiliarBookData.get(player);
            List<String> removed = data.removeUnresolvableEntities(player.getName().getString());
            if (!removed.isEmpty()) {
                for (String name : removed) {
                    player.sendSystemMessage(Component.translatable(
                            "bookoffamiliars.familiar_removed_missing_mod", name
                    ).withStyle(style -> style.withColor(0xFF5555)));
                }
                FamiliarBookData.save(player, data);
            }
        }

        PendingRecoveryData pending = PendingRecoveryData.get(server.overworld());
        if (!pending.hasPending(player.getUUID())) return;

        List<RecoveringFamiliar> entries = pending.drainPending(player.getUUID());
        FamiliarBookData data = FamiliarBookData.get(player);

        for (RecoveringFamiliar rf : entries) {
            if (data.isFull(Config.MAX_FAMILIARS.get())) {
                pending.addPending(player.getUUID(), rf);
                player.sendSystemMessage(Component.translatable(
                        "bookoffamiliars.familiar_recovering_full", rf.displayName()).withStyle(style -> style.withColor(0xFFAA00)));
            } else {
                data.addRecovering(rf);
                player.sendSystemMessage(Component.translatable(
                        "bookoffamiliars.familiar_recovering", rf.displayName()).withStyle(style -> style.withColor(0xFFAA00)));
            }
        }
        FamiliarBookData.save(player, data);
    }

    @SubscribeEvent
    public static void onFamiliarDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;

        UUID entityUUID = event.getEntity().getUUID();
        ReleasedFamiliarTracker tracker = ReleasedFamiliarTracker.get(server.overworld());

        if (!tracker.isTracked(entityUUID)) return;

        ReleasedFamiliarTracker.ReleasedEntry entry = tracker.getEntry(entityUUID);
        tracker.remove(entityUUID);

        long cooldownTicks = (long) Config.RESURRECTION_COOLDOWN_MINUTES.get() * 60L * 20L;
        long recoverAt = event.getEntity().level().getGameTime() + cooldownTicks;

        RecoveringFamiliar rf = RecoveringFamiliar.from(entry.snapshot(), entityUUID, recoverAt);

        ServerPlayer player = server.getPlayerList().getPlayer(entry.playerUUID());
        if (player != null) {
            FamiliarBookData data = FamiliarBookData.get(player);

            if (data.isFull(Config.MAX_FAMILIARS.get())) {
                PendingRecoveryData.get(server.overworld()).addPending(entry.playerUUID(), rf);
                player.sendSystemMessage(Component.translatable(
                        "bookoffamiliars.familiar_recovering_full", entry.snapshot().displayName()).withStyle(style -> style.withColor(0xFFAA00)));
            } else {
                data.addRecovering(rf);
                player.sendSystemMessage(Component.translatable(
                        "bookoffamiliars.familiar_recovering", entry.snapshot().displayName()).withStyle(style -> style.withColor(0xFFAA00)));
            }
            FamiliarBookData.save(player, data);
        } else {
            PendingRecoveryData.get(server.overworld()).addPending(entry.playerUUID(), rf);
            BookOfFamiliarsMod.LOGGER.debug(
                    "Familiar {} died while owner {} was offline. Stored in pending recovery.",
                    entityUUID, entry.playerUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter < 100) return;
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        PendingRecoveryData pending = PendingRecoveryData.get(server.overworld());
        long currentGameTime = server.overworld().getGameTime();
        int maxFamiliars = Config.MAX_FAMILIARS.get();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FamiliarBookData data = FamiliarBookData.get(player);
            boolean changed = false;
            int promoted = data.promoteReadyFamiliars(currentGameTime, maxFamiliars);

            if (promoted > 0) {
                changed = true;
                player.sendSystemMessage(Component.translatable("bookoffamiliars.familiar_returned", promoted).withStyle(style -> style.withColor(0x55FF55)));
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
                                "bookoffamiliars.familiar_pending_moved", rf.displayName()).withStyle(style -> style.withColor(0xFFAA00)));
                    }
                }
            }

            if (changed) {
                FamiliarBookData.save(player, data);
                ModNetwork.CHANNEL.send(
                        new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), currentGameTime),
                        PacketDistributor.PLAYER.with(player));
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        tickCounter = 0;
    }
}
