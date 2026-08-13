package com.emipokemon.client.shop;

import com.emipokemon.shop.ShopSnapshot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

final class ShopScreen extends Screen {
    private final Screen parent;
    private final ShopSnapshot snapshot;
    private final String requestedCategory;
    private final String requestedProduct;
    private final String resultMessage;
    private final boolean resultSuccess;
    private final List<ShopProductButton> productButtons = new ArrayList<>();
    private String categoryId;
    private String productId;
    private int quantity = 1;
    private String pendingProduct = "";
    private int pendingQuantity;
    private long pendingUntil;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    ShopScreen(Screen parent, ShopSnapshot snapshot, String category, String product,
               String resultMessage, boolean resultSuccess) {
        super(Text.literal("Poké Mart de Emi"));
        this.parent = parent;
        this.snapshot = snapshot;
        this.requestedCategory = category == null ? "" : category;
        this.requestedProduct = product == null ? "" : product;
        this.resultMessage = resultMessage == null ? "" : resultMessage;
        this.resultSuccess = resultSuccess;
    }

    Screen parent() {
        return parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(690, width - 20);
        panelHeight = Math.min(390, height - 20);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        resolveSelection();

        addDrawableChild(new ShopButtonWidget(panelX + panelWidth - 30, panelY + 13, 18, 18,
                Text.literal("×"), this::close));
        int categoryY = panelY + 63;
        for (ShopSnapshot.CategoryView category : snapshot.categories) {
            String id = category.id;
            addDrawableChild(new ShopButtonWidget(panelX + 15, categoryY, 112, 25,
                    Text.literal(category.title), () -> selectCategory(id), () -> id.equals(categoryId)));
            categoryY += 31;
        }

        productButtons.clear();
        ShopSnapshot.CategoryView category = category();
        if (category != null) {
            int productY = panelY + 63;
            for (ShopSnapshot.ProductView product : category.products) {
                String id = product.id;
                ShopProductButton button = new ShopProductButton(panelX + 140, productY, 292, 36, product,
                        () -> selectProduct(id), () -> id.equals(productId));
                productButtons.add(button);
                addDrawableChild(button);
                productY += 41;
            }
        }

        ShopSnapshot.ProductView product = product();
        if (product == null) return;
        quantity = Math.max(1, Math.min(quantity, product.maxPerPurchase));
        int controlsX = panelX + 457;
        int controlsY = panelY + panelHeight - 105;
        addDrawableChild(new ShopButtonWidget(controlsX, controlsY, 43, 21, Text.literal("−10"), () -> adjustQuantity(-10)));
        addDrawableChild(new ShopButtonWidget(controlsX + 48, controlsY, 37, 21, Text.literal("−1"), () -> adjustQuantity(-1)));
        addDrawableChild(new ShopButtonWidget(controlsX + 90, controlsY, 37, 21, Text.literal("+1"), () -> adjustQuantity(1)));
        addDrawableChild(new ShopButtonWidget(controlsX + 132, controlsY, 43, 21, Text.literal("+10"), () -> adjustQuantity(10)));
        boolean confirming = product.id.equals(pendingProduct) && quantity == pendingQuantity
                && System.currentTimeMillis() <= pendingUntil;
        ShopButtonWidget buy = new ShopButtonWidget(controlsX, controlsY + 31, 175, 25,
                Text.literal(confirming ? "Confirmar compra" : "Comprar"), this::purchase);
        buy.active = snapshot.balance >= total(product, quantity);
        addDrawableChild(buy);
    }

    private void resolveSelection() {
        if (categoryId == null || categoryById(categoryId) == null) {
            categoryId = categoryById(requestedCategory) != null ? requestedCategory
                    : snapshot.categories.isEmpty() ? "" : snapshot.categories.getFirst().id;
        }
        ShopSnapshot.CategoryView category = category();
        if (category == null || category.products.isEmpty()) {
            productId = "";
            return;
        }
        if (productId == null || productById(category, productId) == null) {
            productId = productById(category, requestedProduct) != null ? requestedProduct : category.products.getFirst().id;
        }
    }

    private void selectCategory(String id) {
        categoryId = id;
        productId = "";
        quantity = 1;
        clearConfirmation();
        clearAndInit();
    }

    private void selectProduct(String id) {
        productId = id;
        quantity = 1;
        clearConfirmation();
        clearAndInit();
    }

    private void adjustQuantity(int amount) {
        ShopSnapshot.ProductView product = product();
        if (product == null) return;
        quantity = Math.max(1, Math.min(product.maxPerPurchase, quantity + amount));
        clearConfirmation();
        clearAndInit();
    }

    private void purchase() {
        ShopSnapshot.ProductView product = product();
        if (product == null) return;
        long total = total(product, quantity);
        long now = System.currentTimeMillis();
        if (total >= snapshot.confirmationThreshold
                && (!product.id.equals(pendingProduct) || pendingQuantity != quantity || now > pendingUntil)) {
            pendingProduct = product.id;
            pendingQuantity = quantity;
            pendingUntil = now + 5_000L;
            clearAndInit();
            return;
        }
        clearConfirmation();
        ShopClient.buy(categoryId, product.id, quantity);
    }

    private void clearConfirmation() {
        pendingProduct = "";
        pendingQuantity = 0;
        pendingUntil = 0L;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA80A0710);
        drawPanel(context);
        drawDetails(context);
        super.render(context, mouseX, mouseY, delta);
        for (ShopProductButton button : productButtons) {
            if (button.isHovered() && !button.stack().isEmpty()) {
                context.drawItemTooltip(textRenderer, button.stack(), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // The shop draws its own dim layer. Calling the vanilla implementation
        // here would blur the already-rendered panel and text under Cobbleverse.
    }

    private void drawPanel(DrawContext context) {
        context.fill(panelX + 6, panelY + 8, panelX + panelWidth + 6, panelY + panelHeight + 8, 0x82000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF5140C19);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 4, 0xFFFF9DDE);
        context.fill(panelX, panelY + 4, panelX + panelWidth, panelY + 7, 0xFF75418E);
        context.fill(panelX, panelY + panelHeight - 4, panelX + panelWidth, panelY + panelHeight, 0xFF75418E);
        context.fill(panelX, panelY, panelX + 3, panelY + panelHeight, 0xFF9D5AAF);
        context.fill(panelX + panelWidth - 3, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF9D5AAF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Poké Mart de Emi"),
                panelX + panelWidth / 2, panelY + 19, 0xFFFFD8F1);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Todo para comenzar tu aventura Pokémon"),
                panelX + panelWidth / 2, panelY + 33, 0xFFBDA2BC);
        context.fill(panelX + 13, panelY + 51, panelX + panelWidth - 13, panelY + 52, 0x706A3C75);

        context.fill(panelX + 136, panelY + 58, panelX + 437, panelY + panelHeight - 18, 0x7627162E);
        context.fill(panelX + 447, panelY + 58, panelX + panelWidth - 15, panelY + panelHeight - 18, 0x8E28152F);
        context.drawTextWithShadow(textRenderer, Text.literal("CATEGORÍAS"), panelX + 20, panelY + 50, 0xFFEAB7DD);
    }

    private void drawDetails(DrawContext context) {
        ShopSnapshot.ProductView product = product();
        if (product == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No hay productos disponibles."),
                    panelX + panelWidth / 2, panelY + 120, 0xFFFFCDE9);
            return;
        }
        int x = panelX + 461;
        int right = panelX + panelWidth - 29;
        ItemStack stack = stack(product);
        context.drawTextWithShadow(textRenderer, Text.literal("TU COMPRA"), x, panelY + 68, 0xFFF5A8D5);
        context.fill(x, panelY + 86, right, panelY + 142, 0xA53B2144);
        if (!stack.isEmpty()) context.drawItem(stack, x + 10, panelY + 105);
        String name = stack.isEmpty() ? product.itemId : stack.getName().getString();
        drawWrapped(context, name, x + 34, panelY + 97, right - x - 40, 0xFFFFE8F7, 2);

        context.drawTextWithShadow(textRenderer, Text.literal("Precio por unidad"), x, panelY + 155, 0xFFBDA2BC);
        context.drawTextWithShadow(textRenderer, Text.literal(product.price + " Michicoins"), x, panelY + 169, 0xFFFFD36A);
        if (product.basePrice != product.price) {
            context.drawTextWithShadow(textRenderer, Text.literal("Descuento de rango: " + snapshot.discountPercent + "%"),
                    x, panelY + 183, 0xFF9DE6B3);
        }
        if (product.description != null && !product.description.isBlank()) {
            context.drawTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(product.description, right - x)),
                    x, panelY + 197, 0xFFE1BCD8);
        }
        context.drawTextWithShadow(textRenderer, Text.literal("Cantidad: " + quantity + " / " + product.maxPerPurchase),
                x, panelY + 212, 0xFFFFE6F6);
        context.drawTextWithShadow(textRenderer, Text.literal("Total: " + total(product, quantity) + " Michicoins"),
                x, panelY + 227, 0xFFFFD36A);
        context.drawTextWithShadow(textRenderer, Text.literal("Saldo: " + snapshot.balance),
                x, panelY + 242, snapshot.balance >= total(product, quantity) ? 0xFFB7EBC5 : 0xFFFF8E9E);

        if (!resultMessage.isBlank()) {
            drawWrapped(context, resultMessage, x, panelY + panelHeight - 40, right - x,
                    resultSuccess ? 0xFF9FE8B5 : 0xFFFF9BA9, 2);
        } else if (total(product, quantity) >= snapshot.confirmationThreshold) {
            context.drawTextWithShadow(textRenderer, Text.literal("Esta compra requiere confirmación."),
                    x, panelY + panelHeight - 31, 0xFFE8C17C);
        }
    }

    private void drawWrapped(DrawContext context, String value, int x, int y, int width, int color, int maxLines) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(value), width);
        for (int index = 0; index < Math.min(maxLines, lines.size()); index++) {
            context.drawTextWithShadow(textRenderer, lines.get(index), x, y + index * 11, color);
        }
    }

    private ShopSnapshot.CategoryView category() {
        return categoryById(categoryId);
    }

    private ShopSnapshot.CategoryView categoryById(String id) {
        if (id == null) return null;
        return snapshot.categories.stream().filter(category -> id.equals(category.id)).findFirst().orElse(null);
    }

    private ShopSnapshot.ProductView product() {
        return productById(category(), productId);
    }

    private ShopSnapshot.ProductView productById(ShopSnapshot.CategoryView category, String id) {
        if (category == null || id == null) return null;
        return category.products.stream().filter(product -> id.equals(product.id)).findFirst().orElse(null);
    }

    private ItemStack stack(ShopSnapshot.ProductView product) {
        Identifier id = Identifier.tryParse(product.itemId);
        return id == null ? ItemStack.EMPTY : new ItemStack(Registries.ITEM.get(id));
    }

    private long total(ShopSnapshot.ProductView product, int count) {
        try {
            return Math.multiplyExact(product.price, count);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
