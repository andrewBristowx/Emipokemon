package com.emipokemon.client.npc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class AdminButtonWidget extends ButtonWidget {
    public AdminButtonWidget(int x, int y, int width, int height, Text label, Runnable action) {
        super(x, y, width, height, label, button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int right = getX() + width;
        int bottom = getY() + height;
        boolean highlighted = active && isHovered();
        int outer = active ? (highlighted ? 0xFFFFC8EA : 0xFFD77FC1) : 0xFF6E566B;
        int inner = active ? (highlighted ? 0xFF71336F : 0xFF45213F) : 0xFF2C242D;
        int text = active ? 0xFFFFE7F7 : 0xFF9D909A;
        context.fill(getX() + 2, getY() + 3, right + 2, bottom + 3, 0x78000000);
        context.fill(getX(), getY(), right, bottom, outer);
        context.fill(getX() + 1, getY() + 1, right - 1, bottom - 1, inner);
        context.fill(getX() + 2, getY() + 2, right - 2, getY() + 4,
                active ? 0xFFB760A4 : 0xFF51434F);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, text);
    }
}
