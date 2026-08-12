package com.emipokemon.client.rewards;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

final class PassButtonWidget extends ButtonWidget {
    PassButtonWidget(int x, int y, int width, int height, Text message, PressAction action) {
        super(x, y, width, height, message, action, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int fill = isHovered() ? 0xEE8C285F : 0xDD25142F;
        context.fill(getX(), getY(), getX() + width, getY() + height, fill);
        int line = isHovered() ? 0xFFFFFFFF : 0xFFFFC8E7;
        context.fill(getX(), getY(), getX() + width, getY() + 1, line);
        context.fill(getX(), getY() + height - 1, getX() + width, getY() + height, line);
        context.fill(getX(), getY(), getX() + 1, getY() + height, line);
        context.fill(getX() + width - 1, getY(), getX() + width, getY() + height, line);
        var renderer = MinecraftClient.getInstance().textRenderer;
        context.drawCenteredTextWithShadow(renderer, getMessage(), getX() + width / 2,
                getY() + (height - 8) / 2, active ? 0xFFFFFFFF : 0xFF8F8190);
    }
}
