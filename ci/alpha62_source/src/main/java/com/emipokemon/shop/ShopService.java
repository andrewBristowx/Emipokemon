package com.emipokemon.shop;

import com.emipokemon.progress.JobAccessPolicy;
import com.emipokemon.progress.ProgressionService;
import com.emipokemon.config.ConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopService {
    private static final Gson GSON = new GsonBuilder().create();
    private static final long PURCHASE_COOLDOWN_MILLIS = 350L;

    private final ShopCatalog catalog;
    private final ProgressionService progression;
    private final ConfigManager configManager;
    private final Map<UUID, Long> lastPurchase = new ConcurrentHashMap<>();

    public ShopService(ShopCatalog catalog, ProgressionService progression, ConfigManager configManager) {
        this.catalog = catalog;
        this.progression = progression;
        this.configManager = configManager;
    }

    public String snapshotJson(ServerPlayerEntity player) {
        ShopCatalog.Config config = catalog.config();
        ShopSnapshot snapshot = new ShopSnapshot();
        snapshot.balance = progression.balance(player.getUuid());
        snapshot.discountPercent = discountPercent(player, config);
        snapshot.confirmationThreshold = config.expensiveConfirmationThreshold;
        for (ShopCatalog.Category category : config.categories) {
            ShopSnapshot.CategoryView categoryView = new ShopSnapshot.CategoryView();
            categoryView.id = category.id;
            categoryView.title = category.title;
            for (ShopCatalog.Product product : category.products) {
                if (!catalog.available(product) || !canAccess(player, product)) continue;
                ShopSnapshot.ProductView productView = new ShopSnapshot.ProductView();
                productView.id = product.id;
                productView.itemId = product.item;
                productView.description = product.description;
                productView.basePrice = balancedPrice(product.price);
                productView.price = discountedPrice(productView.basePrice, snapshot.discountPercent);
                productView.maxPerPurchase = product.maxPerPurchase;
                categoryView.products.add(productView);
            }
            if (!categoryView.products.isEmpty()) snapshot.categories.add(categoryView);
        }
        return GSON.toJson(snapshot);
    }

    public synchronized PurchaseResult purchase(ServerPlayerEntity player, String productId, int quantity) {
        ShopCatalog.Config config = catalog.config();
        if (!config.enabled) return PurchaseResult.failure("La Poké Mart está cerrada temporalmente.");
        ShopCatalog.Product product = catalog.product(productId);
        if (catalog.forbidden(product) || "protection_emi".equalsIgnoreCase(productId)) {
            return PurchaseResult.failure("La Protección Emi no está a la venta.");
        }
        if (!catalog.available(product) || !canAccess(player, product)) {
            return PurchaseResult.failure("Ese producto ya no está disponible para tu rango.");
        }
        if (quantity < 1 || quantity > product.maxPerPurchase) {
            return PurchaseResult.failure("Cantidad no válida para este producto.");
        }

        long now = System.currentTimeMillis();
        Long previous = lastPurchase.get(player.getUuid());
        if (previous != null && now - previous < PURCHASE_COOLDOWN_MILLIS) {
            return PurchaseResult.failure("Espera un instante antes de volver a comprar.");
        }

        Identifier itemId = Identifier.tryParse(product.item);
        if (itemId == null) return PurchaseResult.failure("El producto está mal configurado.");
        Item item = Registries.ITEM.get(itemId);
        if (item == Items.AIR) return PurchaseResult.failure("El objeto no existe en este modpack.");
        ItemStack template = new ItemStack(item);
        if (!hasCapacity(player, template, quantity)) {
            return PurchaseResult.failure("No tienes espacio suficiente para recibir toda la compra.");
        }

        long unitPrice = discountedPrice(balancedPrice(product.price), discountPercent(player, config));
        long total;
        try {
            total = Math.multiplyExact(unitPrice, quantity);
        } catch (ArithmeticException exception) {
            return PurchaseResult.failure("El total de la compra es demasiado grande.");
        }
        if (progression.balance(player.getUuid()) < total) {
            return PurchaseResult.failure("No tienes suficientes Michicoins.");
        }

        lastPurchase.put(player.getUuid(), now);
        if (!progression.spend(player, total, "shop:" + product.id + ":qty=" + quantity)) {
            return PurchaseResult.failure("No se pudo cobrar la compra.");
        }

        int remaining = quantity;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, template.getMaxCount());
            ItemStack delivered = new ItemStack(item, stackSize);
            player.getInventory().insertStack(delivered);
            if (!delivered.isEmpty()) {
                player.dropItem(delivered, false);
            }
            remaining -= stackSize;
        }
        return PurchaseResult.success("Compra completada: " + quantity + "× "
                + template.getName().getString() + " por " + total + " Michicoins.");
    }

    private boolean hasCapacity(ServerPlayerEntity player, ItemStack template, int quantity) {
        long capacity = 0L;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack existing = player.getInventory().getStack(slot);
            if (existing.isEmpty()) {
                capacity += template.getMaxCount();
            } else if (ItemStack.areItemsAndComponentsEqual(existing, template)) {
                capacity += Math.max(0, existing.getMaxCount() - existing.getCount());
            }
            if (capacity >= quantity) return true;
        }
        return false;
    }

    private int discountPercent(ServerPlayerEntity player, ShopCatalog.Config config) {
        Set<String> groups = JobAccessPolicy.groupsFor(player);
        boolean donator = groups.stream().map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals("michidonador") || value.equals("michimod") || value.equals("michidueña") || value.equals("michiduena"));
        if (donator) return config.donatorDiscountPercent;
        boolean vip = groups.stream().map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals("michivip"));
        return vip ? config.vipDiscountPercent : 0;
    }

    private boolean canAccess(ServerPlayerEntity player, ShopCatalog.Product product) {
        if (product == null || product.requiredPermission == null || product.requiredPermission.isBlank()) return true;
        return player.hasPermissionLevel(4) || JobAccessPolicy.hasPermission(player, product.requiredPermission);
    }

    private long discountedPrice(long basePrice, int discountPercent) {
        long multiplier = Math.max(0, 100 - discountPercent);
        return Math.max(1L, (basePrice * multiplier + 99L) / 100L);
    }

    private long balancedPrice(long basePrice) {
        return configManager.get().balance.scaled(basePrice, configManager.get().balance.shopPriceMultiplier);
    }

    public record PurchaseResult(boolean success, String message) {
        public static PurchaseResult success(String message) {
            return new PurchaseResult(true, message);
        }

        public static PurchaseResult failure(String message) {
            return new PurchaseResult(false, message);
        }
    }
}
