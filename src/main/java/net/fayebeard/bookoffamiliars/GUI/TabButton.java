package net.fayebeard.bookoffamiliars.GUI;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class TabButton extends Button {

    private final Identifier texture;
    private final int textureX;
    private final int activeTextureY;
    private final int inactiveTextureY;
    private final int textureWidth;
    private final int textureHeight;
    private final int textureSheetWidth;
    private final int textureSheetHeight;
    private boolean active;

    public TabButton(int x, int y, int width, int height,
                     Identifier texture,
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
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int currentY = active ? activeTextureY : inactiveTextureY;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(),
                textureX, currentY,
                textureWidth, textureHeight,
                textureSheetWidth, textureSheetHeight);
    }

    @Override
    public void updateWidgetNarration(@NonNull NarrationElementOutput output) {
    }
}
