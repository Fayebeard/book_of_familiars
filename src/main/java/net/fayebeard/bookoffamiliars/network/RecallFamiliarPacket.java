package net.fayebeard.bookoffamiliars.network;

import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.ReleasedFamiliarTracker;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.data.TrackedFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public record RecallFamiliarPacket(UUID familiarUUID) {

    public static void encode(RecallFamiliarPacket packet, FriendlyByteBuf  buf) {
        buf.writeUUID(packet.familiarUUID());
    }

    public static RecallFamiliarPacket decode(FriendlyByteBuf buf) {
        return new RecallFamiliarPacket(buf.readUUID());
    }

    public static void handle(RecallFamiliarPacket packet, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;

        boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
        if (!holdingBook) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        ReleasedFamiliarTracker tracker = ReleasedFamiliarTracker.get(server.overworld());
        ReleasedFamiliarTracker.ReleasedEntry entry = tracker.getEntry(packet.familiarUUID());
        if (entry == null || !entry.playerUUID().equals(player.getUUID())) return;

        FamiliarBookData data = FamiliarBookData.get(player);
        int max = Config.MAX_FAMILIARS.get();
        if (data.isFull(max)) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.book_full", max)
                    .withStyle(style -> style.withColor(0xFF5555)));
            return;
        }

        Entity found = null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity candidate = level.getEntity(packet.familiarUUID());
            if (candidate != null) {
                found = candidate;
                break;
            }
        }

        if (found == null) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.familiar_not_found")
                    .withStyle(style -> style.withColor(0xFF5555)));
            return;
        }

        CompoundTag nbt = new CompoundTag();
        found.save(nbt);

        float currentHealth = 0f, maxHealth = 0f, speed = 0f, attackDamage = 0f;
        boolean hasAttackDamage = false;
        int itemCount = -1;

        if (found instanceof LivingEntity livingEntity) {
            currentHealth = livingEntity.getHealth();
            maxHealth = livingEntity.getMaxHealth();
            speed = (float) livingEntity.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (nbt.contains("Items")) {
                itemCount = nbt.getList("Items", 10).size();
            }
            if (livingEntity.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                attackDamage = (float) livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE);
                hasAttackDamage = true;
            }
        }

        StoredFamiliar oldSnapshot = entry.snapshot();
        StoredFamiliar recalled = new StoredFamiliar(nbt, oldSnapshot.entityType(), oldSnapshot. displayName(),
                currentHealth, maxHealth, speed, attackDamage, hasAttackDamage, itemCount,
                oldSnapshot.revival());

        found.discard();
        tracker.remove(packet.familiarUUID());
        data.addFamiliar(recalled);

        player.playNotifySound(ModSounds.FAMILIAR_STORE.get(), SoundSource.PLAYERS, 0.25f, 1.0f);

        FamiliarBookData.save(player, data);
        long currentGameTime = player.level().getGameTime();
        List<TrackedFamiliar> tracked = tracker.getEntriesForPlayer(player.getUUID(), server);
        ModNetwork.CHANNEL.send(
                new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), tracked, currentGameTime),
                PacketDistributor.PLAYER.with(player)
        );
    }
}
