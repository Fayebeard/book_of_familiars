package net.fayebeard.bookoffamiliars.GUI;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ToggleTexturedButton extends TexturedButton {

    private final int activeTextureX;
    private final int inactiveTextureX;
    private boolean active = true;

    public ToggleTexturedButton(int x, int y, int width, int height,
                                int activeTextureX, int inactiveTextureX,
                                Identifier texture,
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
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int currentTextureX = active ? activeTextureX : inactiveTextureX;
        int currentTextureY = isHovered() ? hoveredTextureY : textureY;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(),
                currentTextureX, currentTextureY,
                textureWidth, textureHeight,
                textureSheetWidth, textureSheetHeight);
    }
}
