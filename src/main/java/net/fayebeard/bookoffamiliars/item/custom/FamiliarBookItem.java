package net.fayebeard.bookoffamiliars.item.custom;

import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.attachment.ModAttachments;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.network.OpenFamiliarBookPacket;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.fayebeard.bookoffamiliars.util.ModUtils;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class FamiliarBookItem extends Item {
    public FamiliarBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onLeftClickEntity(@NonNull ItemStack stack, Player player, @NonNull Entity entity) {
        Level level = player.level();

        if (level.isClientSide()) return true;

        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (Config.ENTITY_BLACKLIST.get().contains(entityId)) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.cannot_be_stored").withStyle(style -> style.withColor(0xFFFF5555)));
            return false;
        }

        FamiliarBookData data = player.getData(ModAttachments.FAMILIAR_DATA);
        int currentTotal = data.getFamiliars().size() + data.getRecovering().size();
        int max = Config.MAX_FAMILIARS.get();
        if (currentTotal >= max) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.book_full", max).withStyle(style -> style.withColor(0xFFFF5555)));
            return true;
        }

        String entityType;
        String displayName;
        CompoundTag nbt;

        if (entity instanceof TamableAnimal tamableAnimal) {
            if (!tamableAnimal.isTame() || !tamableAnimal.isOwnedBy(player)) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar").withStyle(style -> style.withColor(0xFFFF5555)));
                return false;
            }

            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            tamableAnimal.save(output);
            nbt = output.buildResult();

            entityType = tamableAnimal.getType().getDescriptionId();
            displayName = tamableAnimal.hasCustomName() && tamableAnimal.getCustomName() != null
                    ? tamableAnimal.getCustomName().getString()
                    : tamableAnimal.getType().getDescription().getString();

        } else if (entity instanceof AbstractHorse horse) {
            EntityReference<LivingEntity> ownerRef = horse.getOwnerReference();
            if (!horse.isTamed() || (ownerRef != null && !ownerRef.getUUID().equals(player.getUUID()))) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar").withStyle(style -> style.withColor(0xFFFF5555)));
                return false;
            }

            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            horse.save(output);
            nbt = output.buildResult();
            entityType = horse.getType().getDescriptionId();
            displayName = horse.hasCustomName() && horse.getCustomName() != null
                    ? horse.getCustomName().getString()
                    : horse.getType().getDescription().getString();

        } else if (entity instanceof Allay allay) {

            Optional<UUID> likedPlayer = allay.getBrain()
                    .getMemory(MemoryModuleType.LIKED_PLAYER);
            if (likedPlayer.isEmpty() || !likedPlayer.get().equals(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar").withStyle(style -> style.withColor(0xFFFF5555)));
                return false;
            }

            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            allay.save(output);
            nbt = output.buildResult();
            entityType = allay.getType().getDescriptionId();
            displayName = allay.hasCustomName() && allay.getCustomName() != null
                    ? allay.getCustomName().getString()
                    : allay.getType().getDescription().getString();

        } else if (Config.ENTITY_WHITELIST.get().contains(entityId)
                    || entity instanceof CopperGolem
                    || entity instanceof SnowGolem
                    || entity instanceof IronGolem
                    || entity instanceof HappyGhast
                    || entity instanceof Strider) {
            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            entity.save(output);
            nbt = output.buildResult();
            entityType = entity.getType().getDescriptionId();
            displayName = entity.hasCustomName() && entity.getCustomName() != null
                    ? entity.getCustomName().getString()
                    : entity.getType().getDescription().getString();

        } else if (entity instanceof Fox fox) {
            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            fox.save(output);
            nbt = output.buildResult();

            boolean trustsPlayer = false;
            Optional<ListTag> trustedList = nbt.getList("Trusted");
            if (trustedList.isPresent()) {
                for (int i = 0; i < trustedList.get().size(); i++) {
                    try {
                        int[] ints = ((IntArrayTag) trustedList.get().get(i)).getAsIntArray();
                        UUID uuid = UUIDUtil.uuidFromIntArray(ints);
                        if (uuid.equals(player.getUUID())) {
                            trustsPlayer = true;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (!trustsPlayer) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar").withStyle(style -> style.withColor(0xFFFF5555)));
                return false;
            }

            entityType = fox.getType().getDescriptionId();
            displayName = fox.hasCustomName() && fox.getCustomName() != null
                    ? fox.getCustomName().getString()
                    : fox.getType().getDescription().getString();

        } else {
            return false;
        }

        float currentHealth = 0f;
        float maxHealth = 0f;
        float speed = 0f;
        float attackDamage = 0f;
        boolean hasAttackDamage = false;
        int itemCount = -1;

        if (entity instanceof LivingEntity livingEntity) {
            currentHealth = livingEntity.getHealth();
            maxHealth = livingEntity.getMaxHealth();
            speed = (float) livingEntity.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (nbt.contains("Items")) {
                itemCount = nbt.getList("Items").map(ListTag::size).orElse(-1);
            }
            if (livingEntity.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                attackDamage = (float) livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE);
                hasAttackDamage = true;
            }
        }

        StoredFamiliar familiar = new StoredFamiliar(nbt, entityType, displayName, currentHealth, maxHealth, speed, attackDamage, hasAttackDamage, itemCount);
        player.getData(ModAttachments.FAMILIAR_DATA).addFamiliar(familiar);
        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.sendParticles(ParticleTypes.POOF,
                entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
        entity.discard();

        if (player instanceof ServerPlayer serverPlayer) {
            ModUtils.playSound(serverPlayer, ModSounds.FAMILIAR_STORE.get());
            serverPlayer.sendSystemMessage(Component.translatable("bookoffamiliars.familiar_stored", Component.literal(displayName)).withStyle(style -> style.withColor(0xFF55FF55)));
        }

        return true;
    }

    @Override
    public @NonNull InteractionResult use(Level level, @NonNull Player player, @NonNull InteractionHand hand) {
        if (!level.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            FamiliarBookData data = serverPlayer.getData(ModAttachments.FAMILIAR_DATA);
            long currentGameTime = serverPlayer.level().getGameTime();
            PacketDistributor.sendToPlayer(serverPlayer, new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), currentGameTime));
        }
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(@NonNull ItemStack itemStack, @NonNull TooltipContext context, @NonNull TooltipDisplay display, Consumer<Component> builder, @NonNull TooltipFlag tooltipFlag) {
        builder.accept(
                Component.translatable("bookoffamiliars.tooltip")
                        .withStyle(style -> style.withColor(0x7a6a5a))
        );

        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
    }

    @Override
    public int getMaxStackSize(@NonNull ItemStack stack) {
        return 1;
    }
}
