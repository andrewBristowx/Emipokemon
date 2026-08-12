package com.emipokemon.client.emote;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

final class EmiButtonWidget extends ButtonWidget {
    private final BooleanSupplier selected;

    EmiButtonWidget(int x, int y, int width, int height, Text label, Runnable action) {
        this(x, y, width, height, label, action, () -> false);
    }

    EmiButtonWidget(
            int x,
            int y,
            int width,
            int height,
            Text label,
            Runnable action,
            BooleanSupplier selected
    ) {
        super(x, y, width, height, label, button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
        this.selected = selected;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean highlighted = isHovered() || selected.getAsBoolean();
        int x = getX();
        int y = getY();
        int right = x + width;
        int bottom = y + height;

        context.fill(x + 1, y + 2, right + 1, bottom + 2, 0x76000000);
        context.fill(x, y, right, bottom, highlighted ? 0xFFF59BD8 : 0xFF8B4A9C);
        context.fill(x + 1, y + 1, right - 1, bottom - 1, highlighted ? 0xE03A183F : 0xDC24122F);
        context.fill(x + 2, y + 2, right - 2, y + 4, highlighted ? 0x805CC8FF : 0x504D79BE);
        context.fill(x + 2, bottom - 3, right - 2, bottom - 1, highlighted ? 0xB06A2E82 : 0x806A2E82);

        if (selected.getAsBoolean()) {
            context.fill(x + 4, bottom - 2, right - 4, bottom, 0xFFFFD6F1);
        }

        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                getMessage(),
                x + width / 2,
                y + (height - 8) / 2,
                highlighted ? 0xFFFFFFFF : 0xFFF3DFF2
        );
    }
}
