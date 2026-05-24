package net.fayebeard.bookoffamiliars.GUI;

import net.fayebeard.bookoffamiliars.network.DeleteFamiliarPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.NonNull;

import java.util.List;

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

        this.addRenderableWidget(Button.builder(Component.literal("✔"), _ -> {
                    ClientPacketDistributor.sendToServer(new DeleteFamiliarPacket(index, isRecovering));
                    Minecraft.getInstance().setScreen(parentScreen);
                })
                .bounds(this.width / 2 - 25, this.height / 2 + 15, 20, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("✖"), _ ->
                        Minecraft.getInstance().setScreen(parentScreen))
                .bounds(this.width / 2 + 5, this.height / 2 + 15, 20, 20)
                .build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        parentScreen.extractBackground(graphics, mouseX, mouseY, a);

        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        graphics.fill(this.width / 2 - 105, this.height / 2 - 30,
                this.width / 2 + 105, this.height / 2 + 45, 0xFF555555);
        graphics.fill(this.width / 2 - 104, this.height / 2 - 29,
                this.width / 2 + 104, this.height / 2 + 44, 0xFFf5f0e8);

        Component line1 = Component.translatable("bookoffamiliars.delete_confirm_line1");

        graphics.text(this.font, line1,
                this.width / 2 - this.font.width(line1) / 2,
                this.height / 2 - 26, 0xFF880000, false);

        String line2 = Component.translatable("bookoffamiliars.delete_confirm_line2", familiarName).getString();
        List<FormattedCharSequence> wrapped = this.font.split(
                Component.literal(line2), 190);
        for (int i = 0; i < wrapped.size(); i++) {
            graphics.text(this.font, wrapped.get(i),
                    this.width / 2 - 95,
                    this.height / 2 - 14 + (i * 10), 0xFF000000, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            Minecraft.getInstance().setScreen(parentScreen);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    @Override
    protected void extractBlurredBackground(@NonNull GuiGraphicsExtractor graphics) {
    }
}
