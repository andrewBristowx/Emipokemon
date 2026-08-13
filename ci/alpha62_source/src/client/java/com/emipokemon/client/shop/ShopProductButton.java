package com.emipokemon.client.shop;

import com.emipokemon.shop.ShopSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.BooleanSupplier;

final class ShopProductButton extends ButtonWidget {
    private final ShopSnapshot.ProductView product;
    private final ItemStack stack;
    private final BooleanSupplier selected;

    ShopProductButton(int x, int y, int width, int height, ShopSnapshot.ProductView product,
                      Runnable action, BooleanSupplier selected) {
        super(x, y, width, height, Text.empty(), button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
        this.product = product;
        Identifier itemId = Identifier.tryParse(product.itemId);
        this.stack = itemId == null ? ItemStack.EMPTY : new ItemStack(Registries.ITEM.get(itemId));
        this.selected = selected;
    }

    ItemStack stack() {
        return stack;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int right = x + width;
        int bottom = y + height;
        boolean chosen = selected.getAsBoolean();
        boolean highlighted = isHovered() || chosen;
        context.fill(x + 2, y + 2, right + 2, bottom + 2, 0x62000000);
        context.fill(x, y, right, bottom, highlighted ? 0xFFD781BD : 0xFF6D3E70);
        context.fill(x + 1, y + 1, right - 1, bottom - 1, highlighted ? 0xEE432047 : 0xE82A1732);
        context.fill(x + 2, y + 2, x + 5, bottom - 2, chosen ? 0xFFFFA6D9 : 0xFF8C508B);
        context.drawItem(stack, x + 9, y + (height - 16) / 2);

        var renderer = MinecraftClient.getInstance().textRenderer;
        String name = stack.isEmpty() ? product.itemId : stack.getName().getString();
        context.drawTextWithShadow(renderer, Text.literal(renderer.trimToWidth(name, width - 95)), x + 31, y + 6, 0xFFFFE8F7);
        String price = product.price + " Michi";
        int priceX = right - renderer.getWidth(price) - 9;
        context.drawTextWithShadow(renderer, Text.literal(price), priceX, y + 19, 0xFFFFD36A);
        if (product.basePrice != product.price) {
            context.drawTextWithShadow(renderer, Text.literal("Oferta"), x + 31, y + 19, 0xFF9DE6B3);
        } else if (product.description != null && !product.description.isBlank()) {
            int descriptionWidth = Math.max(20, priceX - x - 38);
            context.drawTextWithShadow(renderer,
                    Text.literal(renderer.trimToWidth(product.description, descriptionWidth)),
                    x + 31, y + 19, 0xFFCCB4C9);
        }
    }
}
