package net.fayebeard.bookffamiliars.GUI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TexturedButton extends Button {

    private final ResourceLocation texture;
    private final int textureX;
    private final int textureY;
    private final int hoveredTextureY;
    private final int textureWidth;
    private final int textureHeight;
    private final int textureSheetWidth;
    private final int textureSheetHeight;
    private final net.minecraft.network.chat.Component label;
    private final int labelOffsetX;

    public TexturedButton(int x, int y, int width, int height,
                          Component label,
                          int labelOffsetX,
                          ResourceLocation texture,
                          int textureX, int textureY,
                          int hoveredTextureY,
                          int textureWidth, int textureHeight,
                          int textureSheetWidth, int textureSheetHeight,
                          net.minecraft.client.gui.components.Button.OnPress onPress) {
        super( x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.textureX = textureX;
        this.textureY = textureY;
        this.hoveredTextureY = hoveredTextureY;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textureSheetWidth = textureSheetWidth;
        this.textureSheetHeight = textureSheetHeight;
        this.label = label;
        this.labelOffsetX = labelOffsetX;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        int currentTextureY = isHovered() ? hoveredTextureY : textureY;
        pGuiGraphics.blit(texture, getX(), getY(),
                textureX, currentTextureY,
                textureWidth, textureHeight,
                textureSheetWidth, textureSheetHeight);

        pGuiGraphics.drawString(Minecraft.getInstance().font, label,
                getX() + (getWidth() - Minecraft.getInstance().font.width(label)) / 2 + labelOffsetX,
                getY() + (getHeight() - 8) / 2,
                0xFFD700, true);
    }
}
