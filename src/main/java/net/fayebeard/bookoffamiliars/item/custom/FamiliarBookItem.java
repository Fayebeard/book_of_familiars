package net.fayebeard.bookoffamiliars.item.custom;

import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.network.OpenFamiliarBookPacket;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FamiliarBookItem extends Item {
    public FamiliarBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        Level level = player.level();

        if (level.isClientSide()) return true;

        String entityId = entity.getType().builtInRegistryHolder().key().location().toString();
        if (Config.ENTITY_BLACKLIST.get().contains(entityId)) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar"));
            return false;
        }

        FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
        int max = Config.MAX_FAMILIARS.get();
        if (data.getFamiliars().size() >= max) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.book_full", max));
            return true;
        }

        String entityType;
        String displayName;
        CompoundTag nbt = new CompoundTag();

        if (entity instanceof TamableAnimal tamableAnimal) {
            if (!tamableAnimal.isTame() || !tamableAnimal.isOwnedBy(player)) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar"));
                return false;
            }

            tamableAnimal.save(nbt);

            entityType = tamableAnimal.getType().toShortString();
            displayName = tamableAnimal.hasCustomName()
                    ? tamableAnimal.getCustomName().getString()
                    : tamableAnimal.getType().getDescription().getString();
        } else if (entity instanceof AbstractHorse horse) {
            if (!horse.isTamed()) return false;

            UUID ownerUUID = horse.getOwnerUUID();
            if (ownerUUID == null || !ownerUUID.equals(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar"));
                return false;
            }

            horse.save(nbt);
            entityType = horse.getType().toShortString();
            displayName = horse.hasCustomName()
                    ? horse.getCustomName().getString()
                    : horse.getType().getDescription().getString();
        } else if (entity instanceof Allay allay) {
            if (!allay.hasItemInHand()) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar"));
                return false;
            }

            Optional<UUID> likedPlayer = allay.getBrain()
                            .getMemory(MemoryModuleType.LIKED_PLAYER);
            if (likedPlayer.isEmpty() || !likedPlayer.get().equals(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar"));
                return false;
            }

            allay.save(nbt);
            entityType = allay.getType().getDescriptionId();
            displayName = allay.hasCustomName()
                    ? allay.getCustomName().getString()
                    : allay.getType().getDescription().getString();
        } else {
            return false;
        }

        StoredFamiliar familiar = new StoredFamiliar(nbt, entityType, displayName);
        player.getData(ModAttachments.FAMILIAR_DATA).addFamiliar(familiar);
        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.sendParticles(ParticleTypes.POOF,
                entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
        entity.discard();

        player.playNotifySound(ModSounds.FAMILIAR_STORE.get(), SoundSource.PLAYERS, 0.25f, 1.0f);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("bookoffamiliars.familiar_stored", displayName));
        }

        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            List<StoredFamiliar> familiars = serverPlayer.getData(ModAttachments.FAMILIAR_DATA).getFamiliars();
            PacketDistributor.sendToPlayer(serverPlayer, new OpenFamiliarBookPacket(familiars));
            player.playNotifySound(ModSounds.FAMILIAR_BOOK_OPEN.get(), SoundSource.PLAYERS, 0.25f, 1.0f);
        }
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.translatable("bookoffamiliars.tooltip")
                        .withStyle(style -> style.withColor(0x7a6a5a))
        );

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
