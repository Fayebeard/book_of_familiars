package net.fayebeard.bookoffamiliars.item.custom;

import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.network.ModNetwork;
import net.fayebeard.bookoffamiliars.network.OpenFamiliarBookPacket;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
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

        String entityId = Objects.toString(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()), "");
        if (Config.ENTITY_BLACKLIST.get().contains(entityId)) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.cannot_be_stored").withStyle(style -> style.withColor(0xFF5555)));
            return false;
        }

        FamiliarBookData data = FamiliarBookData.get(player);
        int currentTotal = data.getFamiliars().size() + data.getRecovering().size();
        int max = Config.MAX_FAMILIARS.get();
        if (currentTotal >= max) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.book_full", max).withStyle(style -> style.withColor(0xFF5555)));
            return true;
        }

        String entityType;
        String displayName;
        CompoundTag nbt = new CompoundTag();

        if (entity instanceof TamableAnimal tamableAnimal) {
            if (!tamableAnimal.isTame() || !tamableAnimal.isOwnedBy(player)) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar").withStyle(style -> style.withColor(0xFF5555)));
                return false;
            }

            tamableAnimal.save(nbt);
            entityType = tamableAnimal.getType().getDescriptionId();
            displayName = tamableAnimal.hasCustomName() && tamableAnimal.getCustomName() != null
                    ? tamableAnimal.getCustomName().getString()
                    : tamableAnimal.getType().getDescription().getString();

        } else if (entity instanceof AbstractHorse horse) {
            UUID ownerUUID = horse.getOwnerUUID();
            if (!horse.isTamed() || (ownerUUID != null && !ownerUUID.equals(player.getUUID()))) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar").withStyle(style -> style.withColor(0xFF5555)));
                return false;
            }

            horse.save(nbt);
            entityType = horse.getType().getDescriptionId();
            displayName = horse.hasCustomName() && horse.getCustomName() != null
                    ? horse.getCustomName().getString()
                    : horse.getType().getDescription().getString();

        } else if (entity instanceof Allay allay) {

            Optional<UUID> likedPlayer = allay.getBrain()
                    .getMemory(MemoryModuleType.LIKED_PLAYER);
            if (likedPlayer.isEmpty() || !likedPlayer.get().equals(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar").withStyle(style -> style.withColor(0xFF5555)));
                return false;
            }

            allay.save(nbt);
            entityType = allay.getType().getDescriptionId();
            displayName = allay.hasCustomName() && allay.getCustomName() != null
                    ? allay.getCustomName().getString()
                    : allay.getType().getDescription().getString();

        } else if (Config.ENTITY_WHITELIST.get().contains(entityId)
                    || entity instanceof SnowGolem
                    || entity instanceof IronGolem
                    || entity instanceof Strider) {
            entity.save(nbt);
            entityType = entity.getType().getDescriptionId();
            displayName = entity.hasCustomName() && entity.getCustomName() != null
                    ? entity.getCustomName().getString()
                    : entity.getType().getDescription().getString();

        } else if (entity instanceof Fox fox) {
            fox.save(nbt);
            boolean trustsPlayer = false;
            ListTag trustedList = nbt.getList("Trusted", Tag.TAG_INT_ARRAY);
            for (Tag tag : trustedList) {
                UUID uuid = NbtUtils.loadUUID(tag);
                if (uuid.equals(player.getUUID())) {
                    trustsPlayer = true;
                    break;
                }
            }

            if (!trustsPlayer) {
                player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar").withStyle(style -> style.withColor(0xFF5555)));
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
                itemCount = nbt.getList("Items", 10).size();
            }
            if (livingEntity.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                attackDamage = (float) livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE);
                hasAttackDamage = true;
            }
        }

        data.addFamiliar(new StoredFamiliar(nbt, entityType, displayName, currentHealth, maxHealth, speed, attackDamage, hasAttackDamage, itemCount));
        FamiliarBookData.save(player, data);
        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.sendParticles(ParticleTypes.POOF,
                entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
        entity.discard();

        player.playNotifySound(ModSounds.FAMILIAR_STORE.get(), SoundSource.PLAYERS, 0.25f, 1.0f);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("bookoffamiliars.familiar_stored", displayName).withStyle(style -> style.withColor(0x55FF55)));
        }
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        if (!level.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            FamiliarBookData data = FamiliarBookData.get(serverPlayer);
            long currentGameTime = serverPlayer.serverLevel().getGameTime();
            ModNetwork.CHANNEL.send(
                    new OpenFamiliarBookPacket(data.getFamiliars(), data.getRecovering(), currentGameTime),
                    PacketDistributor.PLAYER.with(serverPlayer)
            );
            player.playNotifySound(ModSounds.FAMILIAR_BOOK_OPEN.get(), SoundSource.PLAYERS, 0.25f, 1.0f);
        }
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.translatable("bookoffamiliars.tooltip")
                        .withStyle(style -> style.withColor(0x7a6a5a))
        );

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
