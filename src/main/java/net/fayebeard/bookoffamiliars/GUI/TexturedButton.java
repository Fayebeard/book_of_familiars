package net.fayebeard.bookoffamiliars.GUI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TexturedButton extends Button {

    protected final ResourceLocation texture;
    protected final int textureX;
    protected final int textureY;
    protected final int hoveredTextureY;
    protected final int textureWidth;
    protected final int textureHeight;
    protected final int textureSheetWidth;
    protected final int textureSheetHeight;
    protected final Component label;
    protected final int labelColour;

    public TexturedButton(int x, int y, int width, int height,
                          Component label,
                          ResourceLocation texture,
                          int textureX, int textureY,
                          int hoveredTextureY,
                          int textureWidth, int textureHeight,
                          int textureSheetWidth, int textureSheetHeight,
                          int labelColour,
                          OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.textureX = textureX;
        this.textureY = textureY;
        this.hoveredTextureY = hoveredTextureY;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textureSheetWidth = textureSheetWidth;
        this.textureSheetHeight = textureSheetHeight;
        this.label = label;
        this.labelColour = labelColour;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        int currentTextureY = isHovered() ? hoveredTextureY : textureY;
        pGuiGraphics.blit(texture, getX(), getY(),
                textureX, currentTextureY,
                textureWidth, textureHeight,
                textureSheetWidth, textureSheetHeight);

        pGuiGraphics.drawString(Minecraft.getInstance().font, label,
                getX() + (getWidth() - Minecraft.getInstance().font.width(label)) / 2,
                getY() + (getHeight() - 8) / 2,
                labelColour, true);
    }
}
