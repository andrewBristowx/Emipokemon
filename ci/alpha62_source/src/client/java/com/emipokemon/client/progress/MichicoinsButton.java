package com.emipokemon.client.progress;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.LongSupplier;

/** Inventory shortcut that exposes the synchronized Michicoin balance on hover. */
final class MichicoinsButton extends ButtonWidget {
    private final LongSupplier balance;

    MichicoinsButton(int x, int y, Runnable action, LongSupplier balance) {
        super(x, y, 22, 24, Text.literal("Michicoins"), button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
        this.balance = balance;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        boolean highlighted = isHovered();

        context.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x72000000);
        context.fill(x, y, x + width, y + height, highlighted ? 0xFFFFD987 : 0xFFE8B94F);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, highlighted ? 0xFF704056 : 0xFF4C2941);
        context.fill(x + 2, y + 2, x + width - 2, y + 4, highlighted ? 0xFFFFBFE2 : 0xFFB96599);
        drawCatCoin(context, x + 4, y + 5, highlighted);

        if (isHovered()) {
            long current = balance.getAsLong();
            Text tooltip = current >= 0
                    ? Text.literal("Saldo: " + current + " Michicoins")
                    : Text.literal("Sincronizando Michicoins…");
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private static void drawCatCoin(DrawContext context, int x, int y, boolean highlighted) {
        int rimLight = highlighted ? 0xFFFFFFC7 : 0xFFFFE47A;
        int rim = highlighted ? 0xFFFFD451 : 0xFFE9AD2F;
        int inner = highlighted ? 0xFFFFC038 : 0xFFD99723;
        int stamp = 0xFF633145;

        // Octagonal minted coin with a double rim and reflective pixels.
        context.fill(x + 3, y, x + 11, y + 1, rimLight);
        context.fill(x + 1, y + 2, x + 13, y + 13, rim);
        context.fill(x, y + 4, x + 14, y + 11, rim);
        context.fill(x + 3, y + 14, x + 11, y + 15, 0xFFB96F20);
        context.fill(x + 2, y + 3, x + 12, y + 12, inner);
        context.fill(x + 3, y + 2, x + 11, y + 13, inner);
        context.fill(x + 2, y + 3, x + 6, y + 4, rimLight);
        context.fill(x + 2, y + 4, x + 3, y + 7, rimLight);

        // Cat mint: pointed ears, face, bright eyes, nose, mouth and whiskers.
        context.fill(x + 4, y + 4, x + 10, y + 11, stamp);
        context.fill(x + 3, y + 2, x + 6, y + 6, stamp);
        context.fill(x + 8, y + 2, x + 11, y + 6, stamp);
        context.fill(x + 5, y + 6, x + 6, y + 7, 0xFFFFF1C5);
        context.fill(x + 8, y + 6, x + 9, y + 7, 0xFFFFF1C5);
        context.fill(x + 6, y + 8, x + 8, y + 9, 0xFFFFA8D2);
        context.fill(x + 6, y + 10, x + 8, y + 11, 0xFFFFD770);
        context.fill(x + 2, y + 8, x + 5, y + 9, stamp);
        context.fill(x + 9, y + 8, x + 12, y + 9, stamp);
    }
}
