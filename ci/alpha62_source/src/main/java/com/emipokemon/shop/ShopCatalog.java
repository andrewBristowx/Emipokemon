package com.emipokemon.shop;

import com.emipokemon.Emipokemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ShopCatalog {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_CONFIG_VERSION = 3;
    private static final Set<String> FORBIDDEN_PRODUCT_IDS = Set.of("protection_emi");
    private static final Set<String> FORBIDDEN_ITEM_IDS = Set.of("emiprotecciones:protection_core_emi");
    private final Path file = FabricLoader.getInstance().getConfigDir()
            .resolve(Emipokemon.MOD_ID).resolve("shop").resolve("pokemart.json");
    private volatile Config config = defaults();

    public synchronized void initialize() {
        try {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) {
                config = defaults();
                save(config);
                Emipokemon.LOGGER.info("Created default Poke Mart catalog at {}", file);
            } else {
                reload();
            }
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not initialize Poke Mart; using safe in-memory defaults", exception);
            config = defaults();
        }
    }

    public synchronized boolean reload() {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            if (loaded == null) throw new IllegalArgumentException("The Poke Mart catalog is empty");
            boolean migrated = migrate(loaded);
            boolean sanitized = normalize(loaded);
            config = loaded;
            if (migrated || sanitized) {
                save(loaded);
                Emipokemon.LOGGER.info("Updated Poke Mart catalog to safe version {}", CURRENT_CONFIG_VERSION);
            }
            Emipokemon.LOGGER.info("Reloaded Poke Mart: {} available products, {} unavailable entries",
                    availableProductCount(), unavailableProductCount());
            return true;
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Poke Mart reload failed; keeping the last known-good catalog", exception);
            return false;
        }
    }

    public Config config() {
        return config;
    }

    public Product product(String productId) {
        if (productId == null) return null;
        if (FORBIDDEN_PRODUCT_IDS.contains(productId.trim().toLowerCase(Locale.ROOT))) return null;
        for (Category category : config.categories) {
            for (Product product : category.products) {
                if (product.id.equals(productId) && !forbidden(product)) return product;
            }
        }
        return null;
    }

    public boolean available(Product product) {
        if (product == null || !product.enabled || forbidden(product)) return false;
        Identifier id = Identifier.tryParse(product.item);
        if (id == null) return false;
        Item item = Registries.ITEM.get(id);
        return item != Items.AIR && id.equals(Registries.ITEM.getId(item));
    }

    public int availableProductCount() {
        int count = 0;
        for (Category category : config.categories) {
            for (Product product : category.products) if (available(product)) count++;
        }
        return count;
    }

    public int unavailableProductCount() {
        int count = 0;
        for (Category category : config.categories) {
            for (Product product : category.products) if (product.enabled && !available(product)) count++;
        }
        return count;
    }

    public Path file() {
        return file;
    }

    /** Persists one price without exposing the mutable catalog to a client. */
    public synchronized boolean updatePrice(String productId, long price) {
        Product product = product(productId);
        if (product == null || forbidden(product)) return false;
        long previous = product.price;
        product.price = Math.clamp(price, 1L, 10_000_000L);
        try {
            save(config);
            return true;
        } catch (Exception exception) {
            product.price = previous;
            Emipokemon.LOGGER.error("Could not persist Poke Mart price update for {}", productId, exception);
            return false;
        }
    }

    private void save(Config value) throws Exception {
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(value, writer);
        }
    }

    public boolean forbidden(Product product) {
        return product != null && forbidden(product.id, product.item);
    }

    private static boolean forbidden(String productId, String itemId) {
        String normalizedProductId = productId == null ? "" : productId.trim().toLowerCase(Locale.ROOT);
        String normalizedItemId = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ROOT);
        return FORBIDDEN_PRODUCT_IDS.contains(normalizedProductId) || FORBIDDEN_ITEM_IDS.contains(normalizedItemId);
    }

    private static boolean normalize(Config value) {
        boolean changed = value.version != CURRENT_CONFIG_VERSION;
        value.version = CURRENT_CONFIG_VERSION;
        value.vipDiscountPercent = clamp(value.vipDiscountPercent, 0, 90);
        value.donatorDiscountPercent = clamp(value.donatorDiscountPercent, 0, 90);
        value.expensiveConfirmationThreshold = Math.max(1L, value.expensiveConfirmationThreshold);
        if (value.categories == null) value.categories = new ArrayList<>();
        Map<String, Product> uniqueProducts = new LinkedHashMap<>();
        List<Category> normalizedCategories = new ArrayList<>();
        for (Category category : value.categories) {
            if (category == null) continue;
            category.id = safeId(category.id, "category");
            if (category.title == null || category.title.isBlank()) category.title = category.id;
            if (category.products == null) category.products = new ArrayList<>();
            List<Product> products = new ArrayList<>();
            for (Product product : category.products) {
                if (product == null) continue;
                product.id = safeId(product.id, "product");
                product.item = product.item == null ? "minecraft:air" : product.item.trim().toLowerCase(Locale.ROOT);
                if (forbidden(product.id, product.item)) {
                    changed = true;
                    Emipokemon.LOGGER.warn("Removed forbidden Poke Mart product {} ({})", product.id, product.item);
                    continue;
                }
                product.price = Math.max(1L, Math.min(10_000_000L, product.price));
                product.maxPerPurchase = clamp(product.maxPerPurchase, 1, 2304);
                product.description = product.description == null ? "" : product.description.trim();
                product.requiredPermission = product.requiredPermission == null ? "" : product.requiredPermission.trim();
                if (uniqueProducts.putIfAbsent(product.id, product) == null) products.add(product);
                else Emipokemon.LOGGER.warn("Ignored duplicate Poke Mart product id {}", product.id);
            }
            category.products = products;
            normalizedCategories.add(category);
        }
        value.categories = normalizedCategories;
        return changed;
    }

    private static String safeId(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        return normalized.isBlank() ? fallback : normalized;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static Config defaults() {
        Config value = new Config();
        List<Category> categories = new ArrayList<>(List.of(
                category("balls", "Poké Balls",
                        product("poke_ball", "cobblemon:poke_ball", 12, 64),
                        product("premier_ball", "cobblemon:premier_ball", 16, 64),
                        product("great_ball", "cobblemon:great_ball", 30, 64),
                        product("heal_ball", "cobblemon:heal_ball", 38, 64),
                        product("dusk_ball", "cobblemon:dusk_ball", 55, 64),
                        product("quick_ball", "cobblemon:quick_ball", 70, 64),
                        product("ultra_ball", "cobblemon:ultra_ball", 65, 64)),
                category("medicine", "Medicina",
                        product("potion", "cobblemon:potion", 15, 32),
                        product("super_potion", "cobblemon:super_potion", 35, 32),
                        product("hyper_potion", "cobblemon:hyper_potion", 75, 32),
                        product("full_heal", "cobblemon:full_heal", 40, 32),
                        product("revive", "cobblemon:revive", 90, 16),
                        product("max_potion", "cobblemon:max_potion", 135, 16),
                        product("max_revive", "cobblemon:max_revive", 225, 8)),
                category("battle", "Combate",
                        product("x_attack", "cobblemon:x_attack", 55, 16),
                        product("x_defence", "cobblemon:x_defence", 55, 16),
                        product("x_special_attack", "cobblemon:x_special_attack", 55, 16),
                        product("x_special_defence", "cobblemon:x_special_defence", 55, 16),
                        product("x_speed", "cobblemon:x_speed", 55, 16),
                        product("quick_claw", "cobblemon:quick_claw", 220, 4),
                        product("leftovers", "cobblemon:leftovers", 400, 4)),
                category("evolution", "Evolución",
                        product("fire_stone", "cobblemon:fire_stone", 180, 8),
                        product("water_stone", "cobblemon:water_stone", 180, 8),
                        product("thunder_stone", "cobblemon:thunder_stone", 180, 8),
                        product("leaf_stone", "cobblemon:leaf_stone", 180, 8),
                        product("moon_stone", "cobblemon:moon_stone", 200, 8),
                        product("dawn_stone", "cobblemon:dawn_stone", 220, 8),
                        product("link_cable", "cobblemon:link_cable", 280, 4)),
                category("supplies", "Suministros",
                        product("red_apricorn", "cobblemon:red_apricorn", 18, 64),
                        product("blue_apricorn", "cobblemon:blue_apricorn", 18, 64),
                        product("yellow_apricorn", "cobblemon:yellow_apricorn", 18, 64),
                        product("black_apricorn", "cobblemon:black_apricorn", 18, 64),
                        product("exp_candy_xs", "cobblemon:exp_candy_xs", 25, 32),
                        product("exp_candy_s", "cobblemon:exp_candy_s", 60, 16),
                        product("rare_candy", "cobblemon:rare_candy", 350, 4))
        ));
        categories.addAll(versionTwoCategories());
        value.categories = categories;
        normalize(value);
        return value;
    }

    /** Adds only the new alpha.13 categories, preserving every alpha.12 price edited by the server owner. */
    private static boolean migrate(Config value) {
        if (value.version >= CURRENT_CONFIG_VERSION) return false;
        if (value.categories == null) value.categories = new ArrayList<>();
        Set<String> existing = value.categories.stream()
                .filter(category -> category != null && category.id != null)
                .map(category -> category.id.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        for (Category category : versionTwoCategories()) {
            if (existing.add(category.id)) value.categories.add(category);
        }
        return true;
    }

    private static List<Category> versionTwoCategories() {
        return List.of(
                category("special_balls", "Balls especiales",
                        product("luxury_ball", "cobblemon:luxury_ball", 75, 32),
                        product("timer_ball", "cobblemon:timer_ball", 65, 32),
                        product("repeat_ball", "cobblemon:repeat_ball", 65, 32),
                        product("net_ball", "cobblemon:net_ball", 55, 32),
                        product("dive_ball", "cobblemon:dive_ball", 55, 32),
                        product("love_ball", "cobblemon:love_ball", 85, 16),
                        product("friend_ball", "cobblemon:friend_ball", 90, 16)),
                category("special_evolution", "Evolución +",
                        product("sun_stone", "cobblemon:sun_stone", 200, 8),
                        product("shiny_stone", "cobblemon:shiny_stone", 240, 8),
                        product("dusk_stone", "cobblemon:dusk_stone", 240, 8),
                        product("ice_stone", "cobblemon:ice_stone", 220, 8),
                        product("metal_coat", "cobblemon:metal_coat", 300, 4),
                        product("kings_rock", "cobblemon:kings_rock", 320, 4),
                        product("razor_claw", "cobblemon:razor_claw", 350, 4)),
                category("protections", "Protecciones",
                        product("protection_basic", "emiprotecciones:protection_core", 600, 1,
                                "Área protegida: 21×21 bloques", ""),
                        product("protection_advanced", "emiprotecciones:protection_core_advanced", 1_400, 1,
                                "Área protegida: 31×31 bloques", ""),
                        product("protection_epic", "emiprotecciones:protection_core_epic", 2_800, 1,
                                "Área protegida: 41×41 bloques", ""),
                        product("protection_legendary", "emiprotecciones:protection_core_legendary", 6_000, 1,
                                "Área protegida: 61×61 bloques", "")),
                category("gacha", "Ticket Gacha",
                        product("standard_gacha_ticket", "emipokemon:gacha_ticket", 1_000, 5,
                                "Una tirada en un banner estándar", ""))
        );
    }

    private static Category category(String id, String title, Product... products) {
        Category category = new Category();
        category.id = id;
        category.title = title;
        category.products = new ArrayList<>(List.of(products));
        return category;
    }

    private static Product product(String id, String item, long price, int max) {
        return product(id, item, price, max, "", "");
    }

    private static Product product(String id, String item, long price, int max,
                                   String description, String requiredPermission) {
        Product product = new Product();
        product.id = id;
        product.item = item;
        product.price = price;
        product.maxPerPurchase = max;
        product.description = description;
        product.requiredPermission = requiredPermission;
        return product;
    }

    public static final class Config {
        public int version = CURRENT_CONFIG_VERSION;
        public boolean enabled = true;
        public int vipDiscountPercent = 5;
        public int donatorDiscountPercent = 10;
        public long expensiveConfirmationThreshold = 250L;
        public List<Category> categories = new ArrayList<>();
    }

    public static final class Category {
        public String id = "category";
        public String title = "Categoría";
        public List<Product> products = new ArrayList<>();
    }

    public static final class Product {
        public String id = "product";
        public String item = "minecraft:air";
        public long price = 1L;
        public int maxPerPurchase = 64;
        public boolean enabled = true;
        public String description = "";
        public String requiredPermission = "";
    }
}
