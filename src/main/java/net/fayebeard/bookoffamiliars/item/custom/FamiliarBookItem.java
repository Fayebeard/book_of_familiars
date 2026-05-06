package net.fayebeard.bookoffamiliars.item.custom;

import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.network.ModNetwork;
import net.fayebeard.bookoffamiliars.network.OpenFamiliarBookPacket;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class FamiliarBookItem extends Item {
    public FamiliarBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        Level level = player.level();

        if (level.isClientSide()) return true;

        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()) != null
                ? ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString()
                : "";

        if (Config.ENTITY_BLACKLIST.get().contains(entityId)) {
            player.sendSystemMessage(Component.translatable("bookoffamiliars.not_your_familiar"));
            return false;
        }

        FamiliarBookData data = FamiliarBookData.get(player);

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
                player.sendSystemMessage(
                        Component.translatable("bookoffamiliars.not_your_familiar"));
                return false;
            }

            tamableAnimal.save(nbt);
            entityType = tamableAnimal.getType().getDescriptionId();
            displayName = tamableAnimal.hasCustomName()
                    ? tamableAnimal.getCustomName().getString()
                    : tamableAnimal.getType().getDescription().getString();
        } else if (entity instanceof AbstractHorse horse) {
            if (!horse.isTamed()) return false;
            horse.save(nbt);
            entityType = horse.getType().getDescriptionId();
            displayName = horse.hasCustomName()
                    ? horse.getCustomName().getString()
                    : horse.getType().getDescription().getString();
        } else {
            return false;
        }

        data.addFamiliar(new StoredFamiliar(nbt, entityType, displayName));
        FamiliarBookData.save(player, data);
        entity.discard();
        player.playNotifySound(ModSounds.FAMILIAR_STORE.get(),
                SoundSource.PLAYERS, 0.25f, 1.0f);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("bookoffamiliars.familiar_stored", displayName));
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) pPlayer;
            FamiliarBookData data = FamiliarBookData.get(serverPlayer);

            ModNetwork.CHANNEL.send(
                    new OpenFamiliarBookPacket(data.getFamiliars()),
                    PacketDistributor.PLAYER.with(serverPlayer)
            );
            pPlayer.playNotifySound(ModSounds.FAMILIAR_BOOK_OPEN.get(),
                    SoundSource.PLAYERS, 0.25f, 1.0f);
        }
        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        pTooltipComponents.add(
                Component.translatable("bookoffamiliars.tooltip")
                        .withStyle(style -> style.withColor(0x7a6a5a))
        );
    }
}
