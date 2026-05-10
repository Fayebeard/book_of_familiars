package net.fayebeard.bookoffamiliars.item.custom;

import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.network.OpenFamiliarBookPacket;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.fayebeard.bookoffamiliars.util.ModUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class FamiliarBookItem extends Item {
    public FamiliarBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        Level level = player.level();

        if (level.isClientSide()) return true;

        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (Config.ENTITY_BLACKLIST.get().contains(entityId)) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar"));
            return false;
        }

        FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
        int max = Config.MAX_FAMILIARS.get();
        if (data.getFamiliars().size() >= max) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.book_full",
                    Component.literal(String.valueOf(max))));
            return true;
        }

        String entityType;
        String displayName;
        CompoundTag nbt;

        if (entity instanceof TamableAnimal tamableAnimal) {
            if (!tamableAnimal.isTame() || !tamableAnimal.isOwnedBy(player)) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar"));
                return false;
            }

            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            tamableAnimal.save(output);
            nbt = output.buildResult();

            entityType = tamableAnimal.getType().toShortString();
            displayName = tamableAnimal.hasCustomName()
                    ? tamableAnimal.getCustomName().getString()
                    : tamableAnimal.getType().getDescription().getString();
        } else if (entity instanceof AbstractHorse horse) {
            if (!horse.isTamed()) return false;

            EntityReference<LivingEntity> ownerRef = horse.getOwnerReference();
            if (ownerRef == null || !ownerRef.getUUID().equals(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar"));
                return false;
            }

            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            horse.save(output);
            nbt = output.buildResult();
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

            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            allay.save(output);
            nbt = output.buildResult();
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

        if (player instanceof ServerPlayer serverPlayer) {
            ModUtils.playSound(serverPlayer, ModSounds.FAMILIAR_STORE.get());
            serverPlayer.sendSystemMessage(Component.translatable("bookoffamiliars.familiar_stored",
                    Component.literal(displayName)));
        }

        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            List<StoredFamiliar> familiars = serverPlayer.getData(ModAttachments.FAMILIAR_DATA).getFamiliars();
            PacketDistributor.sendToPlayer(serverPlayer, new OpenFamiliarBookPacket(familiars));

        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(
                Component.translatable("bookoffamiliars.tooltip")
                        .withStyle(style -> style.withColor(0x7a6a5a))
        );

        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
