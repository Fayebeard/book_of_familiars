package net.fayebeard.bookoffamiliars.GUI;

import net.fayebeard.bookoffamiliars.Config;
import net.fayebeard.bookoffamiliars.data.RecoveringFamiliar;
import net.fayebeard.bookoffamiliars.data.StoredFamiliar;
import net.fayebeard.bookoffamiliars.network.ModNetwork;
import net.fayebeard.bookoffamiliars.network.ReleaseFamiliarPacket;
import net.fayebeard.bookoffamiliars.network.SkipRecoveryCooldownPacket;
import net.fayebeard.bookoffamiliars.sounds.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class FamiliarBookScreen extends Screen {

    private sealed interface DisplayEntry permits DisplayEntry.Active, DisplayEntry.Recovering {
        record Active(StoredFamiliar familiar, int familiarIndex) implements DisplayEntry {}
        record Recovering(RecoveringFamiliar familiar, int recoveringIndex) implements DisplayEntry {}
    }

    private final List<StoredFamiliar> familiars = new ArrayList<>();
    private final List<RecoveringFamiliar> recovering = new ArrayList<>();
    private final List<DisplayEntry> displayList = new ArrayList<>();

    private long serverGameTimeAtReceive;
    private long systemTimeAtReceive;

    private Entity currentClientEntity = null;
    private int currentPage = 0;
    private String searchQuery = "";
    private final List<DisplayEntry> dropdownResults = new ArrayList<>();
    private boolean showDropdown = false;
    private int dropdownScrollOffset = 0;

    private static final int MAX_DROPDOWN_ENTRIES = 4;
    private static final int DROPDOWN_ENTRY_HEIGHT = 12;

    private static final ResourceLocation BOOK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("bookoffamiliars", "textures/gui/familiar_book.png");

    private static final int BOOK_WIDTH = 291;
    private static final int BOOK_HEIGHT = 181;
    private int bookX;
    private int bookY;
    private static int savedPage = 0;

    private Button prevButton;
    private Button nextButton;
    private Button releaseButton;
    private Button renameButton;
    private Button skipCooldownButton;
    private Button deleteButton;
    private EditBox searchField;

    private static final Component FAMILIAR_INFO = Component.translatable("bookoffamiliars.familiar_info");
    private static final Component FAMILIAR_STATS = Component.translatable("bookoffamiliars.familiar_stats");
    private static final Component NO_FAMILIARS = Component.translatable("bookoffamiliars.no_familiars_stored");

    private String cachedTimerText = "";
    private long lastTimerSecond = -1;

    public FamiliarBookScreen(List<StoredFamiliar> familiars, List<RecoveringFamiliar> recovering, long currentGameTime) {
        super(Component.translatable("bookoffamiliars.familiar_book_screen"));
        this.familiars.addAll(familiars);
        this.recovering.addAll(recovering);
        this.serverGameTimeAtReceive = currentGameTime;
        this.systemTimeAtReceive = System.currentTimeMillis();
        rebuildDisplayList();
    }

    private void rebuildDisplayList() {
        displayList.clear();
        for (int i = 0; i < familiars.size(); i++) {
            displayList.add(new DisplayEntry.Active(familiars.get(i), i));
        }
        for (int i = 0; i < recovering.size(); i++) {
            displayList.add(new DisplayEntry.Recovering(recovering.get(i), i));
        }
    }

    private long estimatedCurrentGameTime() {
        long elapsedMs = System.currentTimeMillis() - systemTimeAtReceive;
        long elapsedTicks = elapsedMs / 50L;
        return serverGameTimeAtReceive + elapsedTicks;
    }

    @Override
    protected void init() {
        super.init();

        bookX = (this.width - BOOK_WIDTH) / 2;
        bookY = (this.height - BOOK_HEIGHT) / 2;
        currentPage = Math.clamp(savedPage, 0, Math.max(0, displayList.size() - 1));

        prevButton = new TexturedButton(
                bookX + 24, bookY + 148,
                14, 14,
                Component.empty(),
                BOOK_TEXTURE,
                158, 198,
                216,
                14, 14,
                512, 256,
                0,
                btn -> {
                    if (currentPage > 0) {
                        currentPage--;
                        savedPage = currentPage;
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.25f, 1.0f);
                        }
                        discardClientEntity();
                        updateButtonStates();
                    }
                });

        nextButton = new TexturedButton(
                bookX + 253, bookY + 148,
                14, 14,
                Component.empty(),
                BOOK_TEXTURE,
                178, 198,
                216,
                14, 14,
                512, 256,
                0,
                btn -> {
                    if (currentPage < displayList.size() - 1) {
                        currentPage++;
                        savedPage = currentPage;
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.25f, 1.0f);
                        }
                        discardClientEntity();
                    }
                    updateButtonStates();
                });

        releaseButton = new TexturedButton(
                bookX + 171, bookY + 131,
                87, 14,
                Component.translatable("bookoffamiliars.release_button"),
                BOOK_TEXTURE,
                307, 198,
                216,
                87, 14,
                512, 256,
                0xFFD700,
                btn -> {
                    if (!displayList.isEmpty() && displayList.get(currentPage) instanceof DisplayEntry.Active a) {
                        ModNetwork.CHANNEL.send(new ReleaseFamiliarPacket(a.familiarIndex()),
                                PacketDistributor.SERVER.noArg());
                    }
                });

        renameButton = new TexturedButton(
                bookX + 171, bookY + 113,
                87, 14,
                Component.translatable("bookoffamiliars.rename_button"),
                BOOK_TEXTURE,
                208, 198,
                216,
                87, 14,
                512, 256,
                0xFFD700,
                btn -> {
                    if (!displayList.isEmpty()) {
                        savedPage = currentPage;
                        if (displayList.get(currentPage) instanceof DisplayEntry.Active(StoredFamiliar familiar, int familiarIndex)) {
                            Minecraft.getInstance().setScreen(
                                    new RenameScreen(this, familiarIndex, false, familiar.displayName()));
                        } else if (displayList.get(currentPage) instanceof DisplayEntry.Recovering(RecoveringFamiliar familiar, int recoveringIndex)) {
                            Minecraft.getInstance().setScreen(
                                    new RenameScreen(this, recoveringIndex, true, familiar.displayName()));
                        }
                    }
                });

        skipCooldownButton = new TexturedButton(
                bookX + 171, bookY + 131,
                87, 14,
                Component.translatable("bookoffamiliars.skip_cooldown", Config.RESURRECTION_XP_COST.get()),
                BOOK_TEXTURE,
                307, 198,
                216,
                87, 14,
                512, 256,
                0xFFD700,
                btn -> {
                    if (!displayList.isEmpty() && displayList.get(currentPage) instanceof DisplayEntry.Recovering r) {
                        ModNetwork.CHANNEL.send(new SkipRecoveryCooldownPacket(r.recoveringIndex),
                                PacketDistributor.SERVER.noArg());
                    }
                });

        deleteButton = new TexturedButton(
                bookX + 181, bookY + 148,
                67, 14,
                Component.translatable("bookoffamiliars.delete_button"),
                BOOK_TEXTURE,
                409, 198,
                216,
                67, 14,
                512, 256,
                0xFFD700,
                btn -> {
                    if (!displayList.isEmpty()) {
                        savedPage = currentPage;
                        if (displayList.get(currentPage) instanceof DisplayEntry.Active(StoredFamiliar familiar, int familiarIndex)) {
                            Minecraft.getInstance().setScreen(new DeleteConfirmScreen(
                                    this, familiarIndex, false, familiar.displayName()));
                        } else if (displayList.get(currentPage) instanceof DisplayEntry.Recovering(RecoveringFamiliar familiar, int recoveringIndex)) {
                            Minecraft.getInstance().setScreen(new DeleteConfirmScreen(
                                    this, recoveringIndex, true, familiar.displayName()));
                        }
                    }
                });

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

        this.addRenderableWidget(deleteButton);
        this.addRenderableWidget(searchField);
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(releaseButton);
        this.addRenderableWidget(renameButton);
        this.addRenderableWidget(skipCooldownButton);

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
        for (DisplayEntry entry : displayList) {
            String name = switch (entry) {
                case DisplayEntry.Active a -> a.familiar.displayName();
                case DisplayEntry.Recovering r -> r.familiar.displayName();
            };
            String type = switch (entry) {
                case DisplayEntry.Active a -> Component.translatable(a.familiar().entityType()).getString();
                case DisplayEntry.Recovering r -> Component.translatable(r.familiar().entityType()).getString();
            };
            if (name.toLowerCase().contains(query) || type.toLowerCase().contains(query)) {
                dropdownResults.add(entry);
            }
        }
        showDropdown = !dropdownResults.isEmpty();
    }

    private void updateButtonStates() {
        boolean hasEntries = !displayList.isEmpty();
        boolean isRecovering = hasEntries && displayList.get(currentPage) instanceof DisplayEntry.Recovering;

        prevButton.visible = currentPage > 0;
        nextButton.visible = currentPage < displayList.size() - 1;

        releaseButton.visible = hasEntries && !isRecovering;
        renameButton.visible = hasEntries;
        searchField.visible = hasEntries;
        deleteButton.visible = hasEntries;

        skipCooldownButton.visible = isRecovering;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (!displayList.isEmpty()) {
            String pageText = (currentPage + 1) + "/" + displayList.size();
            guiGraphics.drawString(this.font, Component.literal(pageText),
                    bookX + 270 - this.font.width(pageText), bookY + 29, 0x888888, false);
        }

        guiGraphics.drawString(this.font, FAMILIAR_INFO,
                bookX + 77 - this.font.width(FAMILIAR_INFO) / 2, bookY + 20, 0x4A2F6B, false);

        guiGraphics.drawString(this.font, FAMILIAR_STATS,
                bookX + 214 - this.font.width(FAMILIAR_STATS) / 2, bookY + 20, 0x4A2F6B, false);

        if (displayList.isEmpty()) {
            guiGraphics.drawString(this.font, NO_FAMILIARS,
                    bookX + 78 - this.font.width(NO_FAMILIARS) / 2, bookY + 70, 0x000000, false);
        } else {
            DisplayEntry entry = displayList.get(currentPage);
            switch (entry) {
                case DisplayEntry.Active a -> renderActiveFamiliar(guiGraphics, a.familiar(), mouseX, mouseY);
                case DisplayEntry.Recovering r -> renderRecoveringFamiliar(guiGraphics, r.familiar(), mouseX, mouseY);
            }
        }

        if (showDropdown) {
            renderDropdown(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderActiveFamiliar(GuiGraphics guiGraphics, StoredFamiliar familiar, int mouseX, int mouseY) {
        renderEntityPreview(guiGraphics, mouseX, mouseY);
        renderFamiliarNameAndType(guiGraphics, familiar);
        renderStats(guiGraphics, familiar);
    }

    private void renderRecoveringFamiliar(GuiGraphics guiGraphics, RecoveringFamiliar familiar, int mouseX, int mouseY) {
        StoredFamiliar asStored = familiar.toStoredFamiliar();
        renderEntityPreview(guiGraphics, mouseX, mouseY);

        long estimatedNow = estimatedCurrentGameTime();
        long ticksLeft = Math.max(0, familiar.recoverAt() - estimatedNow);
        long secondsLeft = ticksLeft / 20L;
        if (secondsLeft != lastTimerSecond) {
            lastTimerSecond = secondsLeft;
            cachedTimerText = String.format("%d:%02d", secondsLeft / 60L, secondsLeft % 60L);
        }
        Component timerComponent = Component.translatable("bookoffamiliars.recovering_in", cachedTimerText);

        int textWidth = this.font.width(timerComponent);
        int textX = bookX + 29 + (97 - textWidth) / 2;
        int textY = bookY + 30;

        guiGraphics.drawString(this.font, timerComponent, textX, textY, 0xFFAA00, false);

        renderFamiliarNameAndType(guiGraphics, asStored);
        renderStats(guiGraphics, asStored);
    }

    private void renderEntityPreview(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
    }

    private void renderFamiliarNameAndType(GuiGraphics guiGraphics, StoredFamiliar familiar) {
        String displayName = familiar.displayName();
        while (this.font.width(displayName) > 84 && !displayName.isEmpty()) {
            displayName = displayName.substring(0, displayName.length() - 1);
        }
        if (displayName.length() < familiar.displayName().length()) {
            displayName = displayName + "...";
        }
        guiGraphics.drawString(this.font, Component.literal(displayName),
                bookX + 78 - this.font.width(displayName) / 2, bookY + 117, 0x000000, false);

        String entityType = Component.translatable(familiar.entityType()).getString();
        while (this.font.width(entityType) > 60 && !entityType.isEmpty()) {
            entityType = entityType.substring(0, entityType.length() - 1);
        }
        if (entityType.length() < Component.translatable(familiar.entityType()).getString().length()) {
            entityType = entityType + "...";
        }
        guiGraphics.drawString(this.font, Component.literal(entityType),
                bookX + 77 - this.font.width(entityType) / 2, bookY + 133, 0x444444, false);
    }

    private void renderStats(GuiGraphics guiGraphics, StoredFamiliar familiar) {
        if (familiar.maxHealth() == 0f) {
            String[] lines = Component.translatable("bookoffamiliars.stats_outdated").getString().split("\n");
            for (int i = 0; i < lines.length; i++) {
                guiGraphics.drawString(this.font, Component.literal(lines[i]),
                        bookX + 194, bookY + 47 + (i * 12), 0x888888, false);
            }
        } else {
            guiGraphics.drawString(this.font, Component.literal(String.format("%.1f/%.1f", familiar.currentHealth(), familiar.maxHealth())),
                    bookX + 194, bookY + 47, 0x000000, false);

            guiGraphics.drawString(this.font, Component.literal(String.format("%.2f", familiar.speed())),
                    bookX + 194, bookY + 71, 0x000000, false);

            if (familiar.hasAttackDamage()) {
                guiGraphics.drawString(this.font, Component.literal(String.format("%.1f", familiar.attackDamage())),
                        bookX + 194, bookY + 59, 0x000000, false);
            } else {
                guiGraphics.drawString(this.font, Component.literal("N/A"),
                        bookX + 194, bookY + 59, 0x000000, false);
            }

            if (familiar.itemCount() >= 0) {
                guiGraphics.drawString(this.font, Component.literal(String.valueOf(familiar.itemCount())),
                        bookX + 194, bookY + 83, 0xFF000000, false);
            } else {
                guiGraphics.drawString(this.font, Component.literal("N/A"),
                        bookX + 194, bookY + 83, 0x000000, false);
            }
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

            String fullName = switch (dropdownResults.get(resultIndex)) {
                case DisplayEntry.Active a -> a.familiar().displayName();
                case DisplayEntry.Recovering r -> r.familiar().displayName();
            };
            String name = fullName;
            while (this.font.width(name) > dropW - 4 && !name.isEmpty())
                name = name.substring(0, name.length() - 1);
            if (name.length() < fullName.length()) name += "...";

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
                int entryY = dropY + 2 + i * DROPDOWN_ENTRY_HEIGHT;
                if (mouseX >= dropX && mouseX <= dropX + dropW
                        && mouseY >= entryY && mouseY < entryY + DROPDOWN_ENTRY_HEIGHT) {
                    int targetPage = displayList.indexOf(dropdownResults.get(i + dropdownScrollOffset));
                    if (targetPage >= 0) {
                        currentPage = targetPage;
                        discardClientEntity();
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
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.blit(BOOK_TEXTURE, bookX, bookY, 128, 2, BOOK_WIDTH, BOOK_HEIGHT, 512, 256);

        if (!displayList.isEmpty() && displayList.get(currentPage) instanceof DisplayEntry.Recovering) {
            guiGraphics.blit(BOOK_TEXTURE, bookX + 130, bookY,
                    116, 197,
                    11, 34,
                    512, 256);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        savedPage = currentPage;
        discardClientEntity();

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(ModSounds.FAMILIAR_BOOK_CLOSE.get(), 0.25f, 1.0f);
        }
        super.onClose();
    }

    public void refresh(List<StoredFamiliar> updatedFamiliars, List<RecoveringFamiliar> updatedRecovering, long currentGameTime) {
        discardClientEntity();
        this.familiars.clear();
        this.familiars.addAll(updatedFamiliars);
        this.recovering.clear();
        this.recovering.addAll(updatedRecovering);
        this.serverGameTimeAtReceive = currentGameTime;
        this.systemTimeAtReceive = System.currentTimeMillis();
        rebuildDisplayList();
        currentPage = Math.clamp(currentPage, 0, Math.max(0, displayList.size() - 1));
        savedPage = currentPage;
        showDropdown = false;
        dropdownResults.clear();
        dropdownScrollOffset = 0;
        updateButtonStates();
    }

    private void discardClientEntity() {
        if (currentClientEntity != null) {
            currentClientEntity.discard();
            currentClientEntity = null;
        }
    }

    private Entity getCurrentClientEntity() {
        if (currentPage < 0 || currentPage >= displayList.size()) return null;
        if (currentClientEntity != null) return currentClientEntity;
        if (Minecraft.getInstance().level == null) return null;

        CompoundTag nbt = switch (displayList.get(currentPage)) {
            case DisplayEntry.Active a -> a.familiar().nbt();
            case DisplayEntry.Recovering r -> r.familiar().nbt();
        };

        currentClientEntity = EntityType.loadEntityRecursive(
                nbt, Minecraft.getInstance().level, e -> e
        );
        return currentClientEntity;
    }
}
