package net.fayebeard.bookoffamiliars.GUI;

import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.network.ReleaseFamiliarPacket;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class FamiliarBookScreen extends Screen {

    private final List<StoredFamiliar> familiars =  new ArrayList<>();
    private Entity currentClientEntity = null;
    private int currentPage = 0;
    private String searchQuery = "";
    private final List<StoredFamiliar> dropdownResults = new ArrayList<>();
    private boolean showDropdown = false;
    private int dropdownScrollOffset = 0;

    private static final int MAX_DROPDOWN_ENTRIES = 4;
    private static final int DROPDOWN_ENTRY_HEIGHT = 12;

    private static final ResourceLocation BOOK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("bookoffamiliars", "textures/gui/familiar_book.png");

    private static final int BOOK_WIDTH = 291;
    private static final int BOOK_HEIGHT = 181;
    int bookX;
    int bookY;
    private static int savedPage = 0;

    private Button prevButton;
    private Button nextButton;
    private Button releaseButton;
    private Button renameButton;
    private EditBox searchField;

    public FamiliarBookScreen(List<StoredFamiliar> familiars) {
        super(Component.translatable("bookoffamiliars.familiar_book_screen"));
        this.familiars.addAll(familiars);
    }

    @Override
    protected void init() {
        super.init();

        bookX = (this.width - BOOK_WIDTH) / 2;
        bookY = (this.height - BOOK_HEIGHT) / 2;
        currentPage = Math.clamp(savedPage, 0, Math.max(0, familiars.size() - 1));

        prevButton = new TexturedButton(
                bookX + 24, bookY + 148,
                14, 14,
                Component.empty(),
                0,
                BOOK_TEXTURE,
                158, 198,
                216,
                14, 14,
                512, 256,
                btn -> {
                    if (currentPage > 0) {
                        currentPage--;
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.25f, 1.0f);
                        }
                        if (currentClientEntity != null) {
                            currentClientEntity.discard();
                            currentClientEntity = null;
                        }
                        updateButtonStates();
                    }
                }
        );

        nextButton = new TexturedButton(
                bookX + 253, bookY + 148,
                14, 14,
                Component.empty(),
                0,
                BOOK_TEXTURE,
                178, 198,
                216,
                14, 14,
                512, 256,
                btn -> {
                    if (currentPage < familiars.size() - 1) {
                        currentPage++;
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.25f, 1.0f);
                        }
                        if (currentClientEntity != null) {
                            currentClientEntity.discard();
                            currentClientEntity = null;
                        }
                    }
                    updateButtonStates();
                }
        );

        releaseButton = new TexturedButton(
                bookX + 171, bookY + 131,
                87, 14,
                Component.translatable("bookoffamiliars.release_button"),
                0,
                BOOK_TEXTURE,
                307, 198,
                216,
                87, 14,
                512, 256,
                btn -> {
                    if (!familiars.isEmpty()) {
                        PacketDistributor.sendToServer(new ReleaseFamiliarPacket(currentPage));
                    }
                }
        );

        renameButton = new TexturedButton(
                bookX + 171, bookY + 113,
                87, 14,
                Component.translatable("bookoffamiliars.rename_button"),
                0,
                BOOK_TEXTURE,
                208, 198,
                216,
                87, 14,
                512, 256,
                btn -> {
                    if (!familiars.isEmpty()) {
                        savedPage = currentPage;
                        Minecraft.getInstance().setScreen(
                                new RenameScreen(this, currentPage, familiars.get(currentPage).displayName()));
                    }
                }
        );

        searchField = new EditBox(this.font, bookX + 58, bookY + 149, 46, 10, Component.translatable("bookoffamiliars.search")) {
            @Override
            public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                if (getValue().isEmpty() && !isFocused()) {
                    guiGraphics.drawString(font, Component.translatable("bookoffamiliars.search")
                            .withStyle(style -> style.withColor(0x8B6914)),
                            getX() + 2, getY() + 1, 0x8B6914, false);
                }
            }
        };
        searchField.setBordered(false);
        searchField.setMaxLength(30);
        searchField.setResponder(text -> {
            searchQuery = text;
            updateDropdown();
        });

        this.addRenderableWidget(searchField);
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(releaseButton);
        this.addRenderableWidget(renameButton);

        updateButtonStates();
    }

    private void updateDropdown() {
        dropdownResults.clear();
        dropdownScrollOffset = 0;

        if (searchQuery.isEmpty()) {
            showDropdown = false;
            return;
        }

        String query = searchQuery.toLowerCase();
        for (StoredFamiliar familiar : familiars) {
            if (familiar.displayName().toLowerCase().contains(query) ||
                Component.translatable(familiar.entityType()).getString().toLowerCase().contains(query)) {
                dropdownResults.add(familiar);
            }
        }
        showDropdown = !dropdownResults.isEmpty();
    }

    private void updateButtonStates() {
        prevButton.visible = currentPage > 0;
        nextButton.visible = currentPage < familiars.size() - 1;
        releaseButton.visible = !familiars.isEmpty();
        renameButton.visible = !familiars.isEmpty();
        searchField.visible = !familiars.isEmpty();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (!familiars.isEmpty()) {
            String pageText = (currentPage + 1) + "/" + familiars.size();
            guiGraphics.drawString(this.font,
                    Component.literal(pageText),
                    bookX + 247 - this.font.width(pageText), bookY + 152, 0x888888, false);
        }

        guiGraphics.drawString(this.font,
                Component.translatable("bookoffamiliars.familiar_info"),
                bookX + 77 - this.font.width(Component.translatable("bookoffamiliars.familiar_info")) / 2, bookY + 20, 0x4A2F6B, false);

        guiGraphics.drawString(this.font,
                Component.translatable("bookoffamiliars.familiar_stats"),
                bookX + 214 - this.font.width(Component.translatable("bookoffamiliars.familiar_stats")) / 2, bookY + 20, 0x4A2F6B, false);

        if (familiars.isEmpty()) {
            guiGraphics.drawString(this.font,
                    Component.translatable("bookoffamiliars.no_familiars_stored"),
                    bookX + 78 - this.font.width(Component.translatable("bookoffamiliars.no_familiars_stored")) / 2, bookY + 70, 0x000000, false);
        } else {
            StoredFamiliar familiar = familiars.get(currentPage);

            Entity entity = getCurrentClientEntity();
            if (entity instanceof LivingEntity livingEntity) {
                float entityHeight = livingEntity.getBbHeight();
                float entityWidth = livingEntity.getBbWidth();
                float maxDimension = Math.max(entityHeight, entityWidth);
                float boxHeight = 105 - 41;
                float scale = Math.min(35, (boxHeight / maxDimension) * 0.5f);

                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        guiGraphics,
                        bookX + 29, bookY + 38,
                        bookX + 126, bookY + 105,
                        (int) scale,
                        0,
                        mouseX, mouseY,
                        livingEntity
                );
            }

            String displayName = familiar.displayName();
            while (this.font.width(displayName) > 84 && !displayName.isEmpty()) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            if (displayName.length() < familiar.displayName().length()) {
                displayName = displayName + "...";
            }
            guiGraphics.drawString(this.font,
                    Component.literal(displayName),
                    bookX + 78 - this.font.width(displayName) / 2, bookY + 117, 0x000000, false);


            String entityType = Component.translatable(familiar.entityType()).getString();
            while (this.font.width(entityType) > 60 && !entityType.isEmpty()) {
                entityType = entityType.substring(0, entityType.length() - 1);
            }
            if (entityType.length() < Component.translatable(familiar.entityType()).getString().length()) {
                entityType = entityType + "...";
            }
            guiGraphics.drawString(this.font,
                    Component.literal(entityType),
            bookX + 77 - this.font.width(entityType) / 2, bookY + 133, 0x444444, false);


            if (familiar.maxHealth() == 0f) {
                String[] lines = Component.translatable("bookoffamiliars.stats_outdated").getString().split("\n");
                for (int i = 0; i < lines.length; i++) {
                    guiGraphics.drawString(this.font,
                            Component.literal(lines[i]),
                            bookX + 194, bookY + 47 + (i * 12), 0x888888, false);
                }
            } else {

                guiGraphics.drawString(this.font,
                        Component.literal(String.format("%.1f/%.1f", familiar.currentHealth(), familiar.maxHealth())),
                        bookX + 194, bookY + 47, 0x000000, false);

                guiGraphics.drawString(this.font,
                        Component.literal(String.format("%.2f", familiar.speed())),
                        bookX + 194, bookY + 71, 0x000000, false);

                if (familiar.hasAttackDamage()) {
                    guiGraphics.drawString(this.font,
                            Component.literal(String.format("%.1f", familiar.attackDamage())),
                            bookX + 194, bookY + 59, 0x000000, false);
                } else {
                    guiGraphics.drawString(this.font,
                            Component.literal("N/A"),
                            bookX + 194, bookY + 59, 0x000000, false);
                }

                if (familiar.itemCount() >= 0) {
                    guiGraphics.drawString(this.font,
                            Component.literal(String.valueOf(familiar.itemCount())),
                            bookX + 194, bookY + 83, 0xFF000000, false);
                } else {
                    guiGraphics.drawString(this.font,
                            Component.literal("N/A"),
                            bookX + 194, bookY + 83, 0x000000, false);
                }
            }
        }

        if (showDropdown) {
            renderDropdown(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderDropdown(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int dropX = bookX + 58;
        int dropY = bookY + 160;
        int dropW = 100;
        int visibleCount = Math.min(dropdownResults.size(), MAX_DROPDOWN_ENTRIES);
        int dropH = visibleCount * DROPDOWN_ENTRY_HEIGHT + 4;

        guiGraphics.fill(dropX - 1, dropY - 1, dropX + dropW + 1, dropY + dropH + 1, 0xFF888888);
        guiGraphics.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xFFf5f0e8);

        if (dropdownScrollOffset > 0) {
            guiGraphics.drawString(this.font, Component.literal("▲"),
                    dropX + dropW - 10, dropY + 2, 0x888888, false);
        }
        if (dropdownScrollOffset + MAX_DROPDOWN_ENTRIES < dropdownResults.size()) {
            guiGraphics.drawString(this.font, Component.literal("▼"),
                    dropX + dropW - 10, dropY + dropH - 10, 0x888888, false);
        }

        for (int i = 0; i < visibleCount; i++) {
            int resultIndex = i + dropdownScrollOffset;
            int entryY = dropY + 2 + i * DROPDOWN_ENTRY_HEIGHT;
            boolean hovered = mouseX >= dropX && mouseX <= dropX + dropW
                    && mouseY >= entryY && mouseY < entryY + DROPDOWN_ENTRY_HEIGHT;

            if (hovered) {
                guiGraphics.fill(dropX, entryY, dropX + dropW, entryY + DROPDOWN_ENTRY_HEIGHT, 0xFFd4c8b0);
            }

            String name = dropdownResults.get(resultIndex).displayName();
            while (this.font.width(name) > dropW - 4 && !name.isEmpty())
                name = name.substring(0, name.length() - 1);
            if (name.length() < dropdownResults.get(resultIndex).displayName().length()) name += "...";

            guiGraphics.drawString(this.font, Component.literal(name),
                    dropX + 2, entryY + 2, 0x000000, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (showDropdown) {
            int maxScroll = Math.max(0, dropdownResults.size() - MAX_DROPDOWN_ENTRIES);
            dropdownScrollOffset -= (int) Math.signum(scrollY);
            dropdownScrollOffset = Math.clamp(dropdownScrollOffset, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showDropdown && button == 0) {
            int dropX = bookX + 58;
            int dropY = bookY + 160;
            int dropW = 100;
            int visibleCount = Math.min(dropdownResults.size(), MAX_DROPDOWN_ENTRIES);

            for (int i = 0; i < visibleCount; i++) {
                int resultIndex = i + dropdownScrollOffset;
                int entryY = dropY + 2 + i * DROPDOWN_ENTRY_HEIGHT;
                if (mouseX >= dropX && mouseX <= dropX + dropW
                        && mouseY >= entryY && mouseY < entryY + DROPDOWN_ENTRY_HEIGHT) {
                    int targetPage = familiars.indexOf(dropdownResults.get(resultIndex));
                    if (targetPage >= 0) {
                        currentPage = targetPage;
                        if (currentClientEntity != null) { currentClientEntity.discard(); currentClientEntity = null; }
                        updateButtonStates();
                    }
                    searchField.setValue("");
                    searchQuery = "";
                    showDropdown = false;
                    dropdownResults.clear();
                    dropdownScrollOffset = 0;
                    return true;
                }
            }
            showDropdown = false;
            dropdownResults.clear();
            return false;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.blit(BOOK_TEXTURE, bookX, bookY, 128, 2, BOOK_WIDTH, BOOK_HEIGHT, 512, 256);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && showDropdown) {
            showDropdown = false;
            dropdownResults.clear();
            dropdownScrollOffset = 0;
            return true;
        }

        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        return switch (keyCode) {
            case 266 -> {
                prevButton.onPress();
                yield true;
            }
            case 267 -> {
                nextButton.onPress();
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        savedPage = currentPage;
        if (currentClientEntity != null) {
            currentClientEntity.discard();
            currentClientEntity = null;
        }

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(ModSounds.FAMILIAR_BOOK_CLOSE.get(), 0.25f, 1.0f);
        }
        super.onClose();
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    public void refresh(List<StoredFamiliar> updatedFamiliars) {
        if (currentClientEntity != null) {
            currentClientEntity.discard();
            currentClientEntity = null;
        }

        this.familiars.clear();
        this.familiars.addAll(updatedFamiliars);

        currentPage = Math.clamp(currentPage, 0, Math.max(0, familiars.size() - 1));
        savedPage = currentPage;
        showDropdown = false;
        dropdownResults.clear();
        dropdownScrollOffset = 0;
        updateButtonStates();
    }

    private Entity getCurrentClientEntity() {
        if (currentPage < 0 || currentPage >= familiars.size()) return null;
        if (currentClientEntity != null) return currentClientEntity;
        if (Minecraft.getInstance().level == null) return null;
        currentClientEntity = EntityType.loadEntityRecursive(
                familiars.get(currentPage).nbt(), Minecraft.getInstance().level, e -> e
        );
        return currentClientEntity;
    }
}
