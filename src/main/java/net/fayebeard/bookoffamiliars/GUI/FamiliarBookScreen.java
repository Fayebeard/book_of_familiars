package net.fayebeard.bookoffamiliars.GUI;

import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.network.ModNetwork;
import net.fayebeard.bookoffamiliars.network.ReleaseFamiliarPacket;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class FamiliarBookScreen extends Screen {

    private final List<StoredFamiliar> familiars = new ArrayList<>();
    private Entity currentClientEntity = null;
    private int currentPage = 0;

    private static final ResourceLocation BOOK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("bookoffamiliars", "textures/gui/familiar_book.png");

    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;

    private PageButton prevButton;
    private PageButton nextButton;
    private Button releaseButton;

    public FamiliarBookScreen(List<StoredFamiliar> familiars) {
        super(Component.translatable("bookoffamiliars.familiar_book_screen"));
        this.familiars.addAll(familiars);
    }

    @Override
    protected void init() {
        super.init();

        int bookX = (this.width - BOOK_WIDTH) / 2;

        prevButton = new PageButton(bookX + 43, 159, false, btn -> {
            if (currentPage > 0) {
                currentPage--;
                if (currentClientEntity != null) {
                    currentClientEntity.discard();
                    currentClientEntity = null;
                }
                updateButtonStates();
            }
        }, true);

        nextButton = new PageButton(bookX + 116, 159, true, btn -> {
            if (currentPage < familiars.size() - 1) {
                currentPage++;
                if (currentClientEntity != null) {
                    currentClientEntity.discard();
                    currentClientEntity = null;
                }
                updateButtonStates();
            }
        }, true);

        releaseButton = Button.builder(Component.translatable("bookoffamiliars.release_button"), btn -> {
                    if (!familiars.isEmpty()) {
                        ModNetwork.CHANNEL.send(new ReleaseFamiliarPacket(currentPage), PacketDistributor.SERVER.noArg());
                    }
                })
                .bounds(bookX + 92 - 25, 159, 50, 16)
                .build();

        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(releaseButton);

        updateButtonStates();
    }

    private void updateButtonStates() {
        prevButton.visible = currentPage > 0;
        nextButton.visible = currentPage < familiars.size() - 1;
        releaseButton.visible = !familiars.isEmpty();
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        int bookX = (this.width - BOOK_WIDTH) / 2;
        int bookY = 2;

        pGuiGraphics.drawString(this.font,
                Component.translatable("bookoffamiliars.familiar_book_screen"),
                bookX + 92 - this.font.width(Component.translatable("bookoffamiliars.familiar_book_screen")) / 2, bookY + 10, 0x000000, false);

        if (familiars.isEmpty()) {
            pGuiGraphics.drawString(this.font,
                    Component.translatable("bookoffamiliars.no_familiars_stored"),
                    bookX + 92 - this.font.width(Component.translatable("bookoffamiliars.no_familiars_stored")) / 2, bookY + BOOK_HEIGHT / 2, 0x000000, false);
        } else {
            StoredFamiliar familiar = familiars.get(currentPage);

            Entity entity = getCurrentClientEntity();
            if (entity instanceof LivingEntity livingEntity) {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        pGuiGraphics,
                        bookX + 46, bookY + 30,
                        bookX + BOOK_WIDTH - 46, bookY + 130,
                        35,
                        0,
                        pMouseX, pMouseY,
                        livingEntity
                );
            }

            pGuiGraphics.drawString(this.font,
                    Component.literal(familiar.displayName()),
                    bookX + 92 - this.font.width(familiar.displayName()) / 2, bookY + 135, 0x000000, false);

            pGuiGraphics.drawString(this.font,
                    Component.translatable(familiar.entityType()),
                    bookX + 92 - this.font.width(Component.translatable(familiar.entityType())) / 2 - 1,
                    bookY + 147, 0x444444, false);

            Component pageMsg = Component.translatable("book.pageIndicator", currentPage + 1, familiars.size());
            int pageMsgWidth = this.font.width(pageMsg);
            pGuiGraphics.drawString(this.font, pageMsg, bookX + 192 - 44 - pageMsgWidth, bookY + 18, 0x000000, false);
        }
    }


    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
        switch (pKeyCode) {
            case 266: prevButton.onPress(); return true;
            case 267: nextButton.onPress(); return true;
            default: return false;
        }
    }

    @Override
    protected void renderBlurredBackground(float pPartialTick) {
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderTransparentBackground(pGuiGraphics);
        pGuiGraphics.blit(BOOK_TEXTURE, (this.width - BOOK_WIDTH) / 2, 2, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, 256, 256);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (currentClientEntity != null) {
            currentClientEntity.discard();
            currentClientEntity = null;
        }

        Minecraft.getInstance().player.playSound(ModSounds.FAMILIAR_BOOK_CLOSE.get(), 0.25f, 1.0f);
        super.onClose();
    }

    public void refresh(List<StoredFamiliar> updatedFamiliars) {
        if (currentClientEntity != null) {
            currentClientEntity.discard();
            currentClientEntity = null;
        }

        this.familiars.clear();
        this.familiars.addAll(updatedFamiliars);

        if (currentPage >= familiars.size() && currentPage > 0) {
            currentPage = familiars.size() - 1;
        }

        updateButtonStates();
    }

    private Entity getCurrentClientEntity() {
        if (currentPage < 0 || currentPage >= familiars.size()) return null;
        if (currentClientEntity != null) currentClientEntity.discard();
        currentClientEntity = EntityType.loadEntityRecursive(
                familiars.get(currentPage).nbt(), Minecraft.getInstance().level, e -> e
        );
        return currentClientEntity;
    }
}
