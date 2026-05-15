package net.fayebeard.bookoffamiliars.GUI;

import net.fayebeard.bookoffamiliars.network.RenameFamiliarPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.NonNull;

public class RenameScreen extends Screen {

    private final FamiliarBookScreen parentScreen;
    private final int familiarIndex;
    private final String currentName;
    private EditBox renameField;

    public RenameScreen(FamiliarBookScreen parentScreen, int familiarIndex, String currentName) {
        super(Component.literal(""));
        this.parentScreen = parentScreen;
        this.familiarIndex = familiarIndex;
        this.currentName = currentName;
    }

    @Override
    protected void init() {
        super.init();

        renameField = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 10, 200, 20,
                Component.literal(""));
        renameField.setMaxLength(50);
        renameField.setValue(currentName);
        renameField.setFocused(true);
        this.addRenderableWidget(renameField);

        this.addRenderableWidget(Button.builder(Component.translatable("bookoffamiliars.confirm_button"), _ -> confirmRename())
                .bounds(this.width / 2 - 50, this.height / 2 + 15, 100, 20)
                .build());
    }

    private void confirmRename() {
        ClientPacketDistributor.sendToServer(new RenameFamiliarPacket(familiarIndex, renameField.getValue()));
        Minecraft.getInstance().setScreen(parentScreen);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 257) {
            confirmRename();
            return true;
        }
        if (event.key() == 256) {
            Minecraft.getInstance().setScreen(parentScreen);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        parentScreen.extractBackground(graphics, mouseX, mouseY, a);

        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        graphics.fill(this.width / 2 - 105, this.height / 2 - 30,
                this.width / 2 + 105, this.height / 2 + 40, 0xFF7a6a5a);
        graphics.fill(this.width / 2 - 104, this.height / 2 - 29,
                this.width / 2 + 104, this.height / 2 + 39, 0xFFf5f0e8);

        graphics.text(this.font, Component.translatable("bookoffamiliars.rename_familiar"),
                this.width / 2 - this.font.width(Component.translatable("bookoffamiliars.rename_familiar")) / 2,
                this.height / 2 - 22, 0xFF000000, false);

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void extractBlurredBackground(@NonNull GuiGraphicsExtractor graphics) {
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }
}
