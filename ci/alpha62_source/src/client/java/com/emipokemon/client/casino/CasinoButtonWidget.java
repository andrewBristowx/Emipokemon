package com.emipokemon.client.casino;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

final class CasinoButtonWidget extends ButtonWidget {
    CasinoButtonWidget(int x, int y, int width, int height, Text label, Runnable action) {
        super(x, y, width, height, label, button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int outer = isHovered() ? 0xFFFFD56A : 0xFFC97A22;
        int inner = isHovered() ? 0xFF6A214F : 0xFF3B1837;
        context.fill(getX() + 2, getY() + 3, getX() + width + 2, getY() + height + 3, 0x78000000);
        context.fill(getX(), getY(), getX() + width, getY() + height, outer);
        context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, inner);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFF0CF);
    }
}
