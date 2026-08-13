package com.emipokemon.client.progress;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

final class QuestBookButton extends ButtonWidget {
    private final BooleanSupplier claimable;

    QuestBookButton(int x, int y, Runnable action, BooleanSupplier claimable) {
        super(x, y, 22, 24, Text.literal("Misiones"), button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
        this.claimable = claimable;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        boolean highlighted = isHovered() || claimable.getAsBoolean();
        context.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x72000000);
        context.fill(x, y, x + width, y + height, highlighted ? 0xFFFFC8E9 : 0xFFD778B8);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, highlighted ? 0xEF5A275F : 0xE63B1D4C);
        context.fill(x + 2, y + 2, x + width - 2, y + 4, highlighted ? 0xFFFFB6DF : 0xFF9D5693);
        drawCatQuestBook(context, x + 3, y + 4, highlighted);
        if (claimable.getAsBoolean()) {
            context.fill(x + 14, y - 2, x + 23, y + 7, 0xFFFFC84A);
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal("!"), x + 18, y - 1, 0xFF4B1838);
        }
        if (isHovered()) {
            String label = claimable.getAsBoolean() ? "Misiones — recompensa disponible" : "Abrir misiones";
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.literal(label), mouseX, mouseY);
        }
    }

    private static void drawCatQuestBook(DrawContext context, int x, int y, boolean highlighted) {
        int pages = highlighted ? 0xFFFFF3DE : 0xFFFFE8D0;
        int pageShade = 0xFFD8BBA9;
        int cover = highlighted ? 0xFFA94F9C : 0xFF743469;
        int coverDark = 0xFF3B173C;
        int gold = claimColor(highlighted);

        // Closed adventure book with visible page edges, raised spine and clasp.
        context.fill(x + 2, y, x + 15, y + 16, coverDark);
        context.fill(x + 3, y + 1, x + 16, y + 14, pages);
        context.fill(x + 3, y + 12, x + 16, y + 14, pageShade);
        context.fill(x, y + 1, x + 14, y + 15, coverDark);
        context.fill(x + 2, y + 1, x + 14, y + 14, cover);
        context.fill(x, y + 2, x + 3, y + 14, highlighted ? 0xFFFFB7DF : 0xFFE77FBD);
        context.fill(x + 3, y + 2, x + 12, y + 3, highlighted ? 0xFFD978BD : 0xFF9A4B8B);
        context.fill(x + 12, y + 7, x + 15, y + 10, gold);

        // Gold cat medallion: ears, rounded face, eyes, nose and tiny whiskers.
        context.fill(x + 4, y + 5, x + 11, y + 11, gold);
        context.fill(x + 4, y + 3, x + 6, y + 7, gold);
        context.fill(x + 9, y + 3, x + 11, y + 7, gold);
        context.fill(x + 5, y + 6, x + 6, y + 7, coverDark);
        context.fill(x + 9, y + 6, x + 10, y + 7, coverDark);
        context.fill(x + 7, y + 8, x + 8, y + 9, 0xFFFFA4CC);
        context.fill(x + 3, y + 8, x + 5, y + 9, 0xFFFFE9A3);
        context.fill(x + 10, y + 8, x + 12, y + 9, 0xFFFFE9A3);
        context.fill(x + 6, y + 10, x + 9, y + 11, coverDark);
    }

    private static int claimColor(boolean highlighted) {
        return highlighted ? 0xFFFFE47A : 0xFFFFC954;
    }
}
