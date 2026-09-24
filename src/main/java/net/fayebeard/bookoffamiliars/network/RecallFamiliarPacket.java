package net.fayebeard.bookoffamiliars.network;

import io.netty.buffer.ByteBuf;
import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.ReleasedFamiliarTracker;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.data.TrackedFamiliar;
import net.fayebeard.bookoffamiliars.item.custom.FamiliarBookItem;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record RecallFamiliarPacket(UUID familiarUUID) implements CustomPacketPayload {

    public static final Type<RecallFamiliarPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BookOfFamiliarsMod.MOD_ID, "recall_familiar"));

    public static final StreamCodec<ByteBuf, RecallFamiliarPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    RecallFamiliarPacket::familiarUUID,
                    RecallFamiliarPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RecallFamiliarPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            boolean holdingBook = player.getMainHandItem().getItem() instanceof FamiliarBookItem
                    || player.getOffhandItem().getItem() instanceof FamiliarBookItem;
            if (!holdingBook) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;

            ReleasedFamiliarTracker tracker = ReleasedFamiliarTracker.get(server.overworld());
            ReleasedFamiliarTracker.ReleasedEntry entry = tracker.getEntry(packet.familiarUUID());
            if (entry == null || !entry.playerUUID().equals(player.getUUID())) return;

            FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
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
            StoredFamiliar recalled = new StoredFamiliar(nbt, oldSnapshot.entityType(), oldSnapshot.displayName(),
                    currentHealth, maxHealth, speed, attackDamage, hasAttackDamage, itemCount,
                    oldSnapshot.revival());

            found.discard();
            tracker.remove(packet.familiarUUID());
            data.addFamiliar(recalled);

            player.playNotifySound(ModSounds.FAMILIAR_STORE.get(), SoundSource.PLAYERS, 0.25f, 1.0f);

            long currentGameTime = player.level().getGameTime();
            List<TrackedFamiliar> tracked = tracker.getEntriesForPlayer(player.getUUID(), server);
            PacketDistributor.sendToPlayer(player, new OpenFamiliarBookPacket(
                    data.getFamiliars(), data.getRecovering(), tracked, currentGameTime));
        });
    }
}
