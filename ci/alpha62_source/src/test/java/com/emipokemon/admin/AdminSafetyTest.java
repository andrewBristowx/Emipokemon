package com.emipokemon.admin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSafetyTest {
    private static String source(String relative) throws Exception {
        return Files.readString(Path.of("src", relative));
    }

    @Test
    void everyAdminPacketIsPermissionCheckedAndServerValidated() throws Exception {
        String network = source("main/java/com/emipokemon/admin/AdminNetworking.java");
        assertTrue(network.contains("if (!player.hasPermissionLevel(4))"));
        assertTrue(network.contains("pokemonCatalog().get(featured.getKey())"));
        assertTrue(network.contains("noneMatch(banner::allows)"));
        assertTrue(network.contains("shopCatalog().updatePrice"));
        assertTrue(network.contains("lookedAtMachine(player)"));
    }

    @Test
    void forbiddenProtectionCannotBeEditedBackIntoShop() throws Exception {
        String shop = source("main/java/com/emipokemon/shop/ShopCatalog.java");
        assertTrue(shop.contains("FORBIDDEN_PRODUCT_IDS = Set.of(\"protection_emi\")"));
        assertTrue(shop.contains("if (product == null || forbidden(product)) return false"));
    }
}
