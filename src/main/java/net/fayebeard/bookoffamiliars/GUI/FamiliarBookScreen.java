package net.fayebeard.bookoffamiliars.GUI;

import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.network.ReleaseFamiliarPacket;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class FamiliarBookScreen extends Screen {

    private final List<StoredFamiliar> familiars = new ArrayList<>();
    private Entity currentClientEntity = null;
    private int currentPage = 0;

    private static final Identifier BOOK_TEXTURE =
            Identifier.fromNamespaceAndPath("bookoffamiliars", "textures/gui/familiar_book.png");

    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;

    private PageButton prevButton;
    private PageButton nextButton;
    private Button releaseButton;
    private Button renameButton;

    public FamiliarBookScreen(List<StoredFamiliar> familiars) {
        super(Component.translatable("bookoffamiliars.familiar_book_screen"));
        this.familiars.addAll(familiars);
    }

    @Override
    protected void init() {
        super.init();

        int bookX = (this.width - BOOK_WIDTH) / 2;
        int bookY = 2;

        prevButton = new PageButton(bookX + 67, bookY + 160, false, btn -> {
            if (currentPage > 0) {
                currentPage--;
                if (currentClientEntity  != null) {
                    currentClientEntity.discard();
                    currentClientEntity = null;
                }
                updateButtonStates();
            }
        }, true);

        nextButton = new PageButton(bookX + 94, bookY + 160, true, btn -> {
            if (currentPage < familiars.size() - 1) {
                currentPage++;
                if (currentClientEntity != null) {
                    currentClientEntity.discard();
                    currentClientEntity = null;
                }
                updateButtonStates();
            }
        }, true);

        releaseButton = new TexturedButton(
                bookX + 40, bookY + 147,
                53, 15,
                Component.translatable("bookoffamiliars.release_button"),
                0,
                BOOK_TEXTURE,
                54, 191,
                207,
                53, 15,
                256, 256,
                btn -> {
                    if (!familiars.isEmpty()) {
                        ClientPacketDistributor.sendToServer(new ReleaseFamiliarPacket(currentPage));
                    }
                }
        );

        renameButton = new TexturedButton(
                bookX + 94, bookY + 147,
                53, 15,
                Component.translatable("bookoffamiliars.rename_button"),
                0,
                BOOK_TEXTURE,
                54, 191,
                207,
                53, 15,
                256, 256,
                btn -> {
                    if (!familiars.isEmpty()) {
                        Minecraft.getInstance().setScreen(
                                new RenameScreen(this, currentPage, familiars.get(currentPage).displayName())
                        );
                    }
                }
        );

        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(releaseButton);
        this.addRenderableWidget(renameButton);

        updateButtonStates();
    }

    private void updateButtonStates() {
        prevButton.visible = currentPage > 0;
        nextButton.visible = currentPage < familiars.size() - 1;
        releaseButton.visible = !familiars.isEmpty();
        renameButton.visible = !familiars.isEmpty();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int bookX = (this.width - BOOK_WIDTH) / 2;
        int bookY = 2;

        graphics.text(this.font,
                Component.translatable("bookoffamiliars.familiar_book_screen"),
                bookX + 92 - this.font.width(Component.translatable("bookoffamiliars.familiar_book_screen")) / 2, bookY + 10, 0xFF000000, false);

        if (familiars.isEmpty()) {
            graphics.text(this.font,
                    Component.translatable("bookoffamiliars.no_familiars_stored"),
                    bookX + 92 - this.font.width(Component.translatable("bookoffamiliars.no_familiars_stored")) / 2, bookY + BOOK_HEIGHT / 2, 0xFF000000, false);
        } else {
            StoredFamiliar familiar = familiars.get(currentPage);

            Entity entity = getCurrentClientEntity();
            if (entity instanceof LivingEntity livingEntity) {
                float entityHeight = livingEntity.getBbHeight();
                float entityWidth = livingEntity.getBbWidth();
                float maxDimension = Math.max(entityHeight, entityWidth);
                float boxHeight = 105 - 41;
                float scale = Math.min(35, (boxHeight / maxDimension) * 0.5f);

                InventoryScreen.extractEntityInInventoryFollowsMouse(
                        graphics,
                        bookX + 44, bookY + 41,
                        bookX + 140, bookY + 105,
                        (int) scale,
                        0,
                        mouseX, mouseY,
                        livingEntity
                );
            }

            String displayName = familiar.displayName();
            while (this.font.width(displayName) > 84 && displayName.length() > 0) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            if (displayName.length() < familiar.displayName().length()) {
                displayName = displayName + "...";
            }
            graphics.text(this.font,
                    Component.literal(displayName),
                    bookX + 92 - this.font.width(displayName) / 2, bookY + 118, 0xFF000000, false);


            String entityType = Component.translatable(familiar.entityType()).getString();
            while (this.font.width(entityType) > 60 && entityType.length() > 0) {
                entityType = entityType.substring(0, entityType.length() - 1);
            }
            if (entityType.length() < Component.translatable(familiar.entityType()).getString().length()) {
                entityType = entityType + "...";
            }
            graphics.text(this.font,
                    Component.literal(entityType),
                    bookX + 93 - this.font.width(entityType) / 2, bookY + 134, 0xFF444444, false);

            Component pageMsg = Component.translatable("book.pageIndicator", currentPage + 1, familiars.size());
            int pageMsgWidth = this.font.width(pageMsg);
            graphics.text(this.font, pageMsg, bookX + 192 - 44 - pageMsgWidth, bookY + 18, 0xFF000000, false);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractTransparentBackground(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE, (this.width - BOOK_WIDTH) / 2, 2, 0.0F, 0.0F, BOOK_WIDTH, BOOK_HEIGHT - 1, 256, 256);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) return true;
        switch (event.key()) {
            case 266: prevButton.onPress(event); return true;
            case 267: nextButton.onPress(event); return true;
            default: return false;
        }
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

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
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
                familiars.get(currentPage).nbt(),
                Minecraft.getInstance().level,
                EntitySpawnReason.LOAD,
                e -> e
        );
        return currentClientEntity;
    }
}
