package net.fayebeard.bookoffamiliars.GUI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TabButton extends Button {

    private final ResourceLocation texture;
    private final int textureX;
    private final int activeTextureY;
    private final int inactiveTextureY;
    private final int textureWidth;
    private final int textureHeight;
    private final int textureSheetWidth;
    private final int textureSheetHeight;
    private boolean active;

    public TabButton(int x, int y, int width, int height,
                     ResourceLocation texture,
                     int textureX,
                     int activeTextureY,
                     int inactiveTextureY,
                     int textureWidth, int textureHeight,
                     int textureSheetWidth, int textureSheetHeight,
                     boolean initiallyActive,
                     Button.OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.textureX = textureX;
        this.activeTextureY = activeTextureY;
        this.inactiveTextureY = inactiveTextureY;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textureSheetWidth = textureSheetWidth;
        this.textureSheetHeight = textureSheetHeight;
        this.active = initiallyActive;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        int currentY = active ? activeTextureY : inactiveTextureY;
        pGuiGraphics.blit(texture, getX(), getY(),
                textureX, currentY,
                textureWidth, textureHeight,
                textureSheetWidth, textureSheetHeight);
    }

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
    }
}
