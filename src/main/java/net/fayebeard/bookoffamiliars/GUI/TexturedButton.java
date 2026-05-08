package net.fayebeard.bookoffamiliars.GUI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class TexturedButton extends Button {

    private final Identifier texture;
    private final int textureX;
    private final int textureY;
    private final int hoveredTextureY;
    private final int textureWidth;
    private final int textureHeight;
    private final int textureSheetWidth;
    private final int textureSheetHeight;
    private final Component label;
    private final int labelOffsetX;

    public TexturedButton(int x, int y, int width, int height,
                          Component label,
                          int labelOffsetX,
                          Identifier texture,
                          int textureX, int textureY,
                          int hoveredTextureY,
                          int textureWidth, int textureHeight,
                          int textureSheetWidth, int textureSheetHeight,
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
        this.labelOffsetX = labelOffsetX;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int currentTextureY = isHovered() ? hoveredTextureY : textureY;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(),
                textureX, currentTextureY,
                textureWidth, textureHeight,
                textureSheetWidth, textureSheetHeight);

        graphics.text(Minecraft.getInstance().font, label,
                getX() + (getWidth() - Minecraft.getInstance().font.width(label)) / 2 + labelOffsetX,
                getY() + (getHeight() - 8) / 2,
                0xFFFFD700, true);
    }
}
