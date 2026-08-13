package com.emipokemon.shop;

import java.util.ArrayList;
import java.util.List;

public final class ShopSnapshot {
    public long balance;
    public int discountPercent;
    public long confirmationThreshold;
    public List<CategoryView> categories = new ArrayList<>();

    public static final class CategoryView {
        public String id = "";
        public String title = "";
        public List<ProductView> products = new ArrayList<>();
    }

    public static final class ProductView {
        public String id = "";
        public String itemId = "minecraft:air";
        public String description = "";
        public long basePrice;
        public long price;
        public int maxPerPurchase;
    }
}
