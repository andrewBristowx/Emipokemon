package com.emipokemon.client.shop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

final class ShopButtonWidget extends ButtonWidget {
    private final BooleanSupplier selected;

    ShopButtonWidget(int x, int y, int width, int height, Text label, Runnable action) {
        this(x, y, width, height, label, action, () -> false);
    }

    ShopButtonWidget(int x, int y, int width, int height, Text label, Runnable action, BooleanSupplier selected) {
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
        int outer = active ? (highlighted ? 0xFFFFC9EB : 0xFFCE78B9) : 0xFF6D5969;
        int inner = active ? (highlighted ? 0xFF6F336E : 0xFF43203F) : 0xFF2B252B;
        int shine = active ? (highlighted ? 0xFFC767AB : 0xFF76406D) : 0xFF51464E;
        int text = active ? (highlighted ? 0xFFFFFFFF : 0xFFFFE6F6) : 0xFF9E929B;

        context.fill(x + 2, y + 3, right + 2, bottom + 3, 0x70000000);
        context.fill(x, y, right, bottom, outer);
        context.fill(x + 1, y + 1, right - 1, bottom - 1, inner);
        context.fill(x + 2, y + 2, right - 2, y + 4, shine);
        context.fill(x + 2, y + 2, x + 6, y + 3, 0xFFFFD8EE);
        context.fill(right - 6, y + 2, right - 2, y + 3, 0xFFFFD8EE);
        if (chosen) context.fill(x + 7, bottom - 2, right - 7, bottom, 0xFFFFA8DB);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                x + width / 2, y + (height - 8) / 2, text);
    }
}
