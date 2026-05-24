package net.fayebeard.bookoffamiliars.GUI;

import net.fayebeard.bookoffamiliars.network.DeleteFamiliarPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class DeleteConfirmScreen extends Screen {

    private final FamiliarBookScreen parentScreen;
    private final int index;
    private final boolean isRecovering;
    private final String familiarName;

    public DeleteConfirmScreen(FamiliarBookScreen parentScreen, int index, boolean isRecovering, String familiarName) {
        super(Component.literal(""));
        this.parentScreen = parentScreen;
        this.index = index;
        this.isRecovering = isRecovering;
        this.familiarName = familiarName;
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(Button.builder(Component.literal("✔"), btn -> {
            PacketDistributor.sendToServer(new DeleteFamiliarPacket(index, isRecovering));
            Minecraft.getInstance().setScreen(parentScreen);
        })
                .bounds(this.width / 2 - 25, this.height / 2 + 15, 20, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("✖"), btn ->
                Minecraft.getInstance().setScreen(parentScreen))
                .bounds(this.width / 2 + 5, this.height / 2 + 15, 20, 20)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        parentScreen.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);

        guiGraphics.fill(this.width / 2 - 105, this.height / 2 - 30,
                this.width / 2 + 105, this.height / 2 + 45, 0xFF555555);
        guiGraphics.fill(this.width / 2 - 104, this.height / 2 - 29,
                this.width / 2 + 104, this.height / 2 + 44, 0xFFf5f0e8);

        Component line1 = Component.translatable("bookoffamiliars.delete_confirm_line1");

        guiGraphics.drawString(this.font, line1,
                this.width / 2 - this.font.width(line1) / 2,
                this.height / 2 - 26, 0x880000, false);

        String line2 = Component.translatable("bookoffamiliars.delete_confirm_line2", familiarName).getString();
        List<FormattedCharSequence> wrapped = this.font.split(
                Component.literal(line2), 190);
        for (int i = 0; i < wrapped.size(); i++) {
            guiGraphics.drawString(this.font, wrapped.get(i),
                    this.width / 2 - 95,
                    this.height / 2 - 14 + (i * 10), 0x000000, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            Minecraft.getInstance().setScreen(parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}
