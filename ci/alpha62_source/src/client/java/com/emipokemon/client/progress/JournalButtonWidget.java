package com.emipokemon.client.progress;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

/** Pixel-styled journal button that stays readable with resource packs and shaders. */
final class JournalButtonWidget extends ButtonWidget {
    private final BooleanSupplier selected;

    JournalButtonWidget(int x, int y, int width, int height, Text label, Runnable action) {
        this(x, y, width, height, label, action, () -> false);
    }

    JournalButtonWidget(
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
        int x = getX();
        int y = getY();
        int right = x + width;
        int bottom = y + height;
        boolean chosen = selected.getAsBoolean();
        boolean highlighted = active && (isHovered() || chosen);

        int outer = active ? (highlighted ? 0xFFFFC8EA : 0xFFD77FC1) : 0xFF6E566B;
        int inner = active ? (highlighted ? 0xFF71336F : 0xFF45213F) : 0xFF2C242D;
        int shine = active ? (highlighted ? 0xFFB760A4 : 0xFF77406E) : 0xFF51434F;
        int text = active ? (highlighted ? 0xFFFFFFFF : 0xFFFFE7F7) : 0xFF9D909A;

        context.fill(x + 2, y + 3, right + 2, bottom + 3, 0x78000000);
        context.fill(x, y, right, bottom, outer);
        context.fill(x + 1, y + 1, right - 1, bottom - 1, inner);
        context.fill(x + 2, y + 2, right - 2, y + 4, shine);

        // Small cream corner ornaments and a paw keep every action in Emi's visual style.
        int ornament = active ? 0xFFFFD8EE : 0xFF776A73;
        context.fill(x + 2, y + 2, x + 6, y + 3, ornament);
        context.fill(right - 6, y + 2, right - 2, y + 3, ornament);
        context.fill(x + 3, bottom - 3, x + 6, bottom - 2, ornament);
        context.fill(right - 6, bottom - 3, right - 3, bottom - 2, ornament);
        if (width >= 58) drawPaw(context, x + 8, y + height / 2, active ? 0xFFFFBADC : 0xFF776A73);

        if (chosen) {
            context.fill(x + 8, bottom - 2, right - 8, bottom, 0xFFFFA7DC);
        }

        int textOffset = width >= 58 ? 4 : 0;
        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                getMessage(),
                x + width / 2 + textOffset,
                y + (height - 8) / 2,
                text
        );
    }

    private static void drawPaw(DrawContext context, int centerX, int centerY, int color) {
        context.fill(centerX - 2, centerY, centerX + 2, centerY + 3, color);
        context.fill(centerX - 3, centerY - 3, centerX - 1, centerY - 1, color);
        context.fill(centerX, centerY - 4, centerX + 2, centerY - 2, color);
        context.fill(centerX + 3, centerY - 2, centerX + 5, centerY, color);
    }
}
