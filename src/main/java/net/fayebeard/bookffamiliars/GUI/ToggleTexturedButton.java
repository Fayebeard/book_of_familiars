package net.fayebeard.bookffamiliars.GUI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ToggleTexturedButton extends TexturedButton {

    private final int activeTextureX;
    private final int inactiveTextureX;
    private boolean active = true;

    public ToggleTexturedButton(int x, int y, int width, int height,
                                int activeTextureX, int inactiveTextureX,
                                ResourceLocation texture,
                                int textureY, int hoveredTextureY,
                                int textureWidth, int textureHeight,
                                int textureSheetWidth, int textureSheetHeight,
                                OnPress onPress) {
        super(x, y, width, height, Component.empty(),
                texture, activeTextureX, textureY, hoveredTextureY,
                textureWidth, textureHeight, textureSheetWidth, textureSheetHeight, 0, onPress);
        this.activeTextureX = activeTextureX;
        this.inactiveTextureX = inactiveTextureX;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int currentTextureX = active ? activeTextureX : inactiveTextureX;
        int currentTextureY = isHovered() ? hoveredTextureY : textureY;
        guiGraphics.blit(texture, getX(), getY(),
                currentTextureX, currentTextureY,
                textureWidth, textureHeight,
                textureSheetWidth, textureSheetHeight);
    }
}
