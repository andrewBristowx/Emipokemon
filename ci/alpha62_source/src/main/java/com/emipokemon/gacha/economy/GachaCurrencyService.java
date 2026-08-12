package com.emipokemon.gacha.economy;

import com.emipokemon.gacha.banner.BannerDefinition;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class GachaCurrencyService {
    public Result withdraw(ServerPlayerEntity player, BannerDefinition.Currency currency) {
        if (currency == null || currency.amount <= 0 || "FREE".equalsIgnoreCase(currency.type)) {
            return Result.success(null, 0);
        }
        if (!"ITEM".equalsIgnoreCase(currency.type)) {
            return Result.failure("Tipo de moneda no soportado: " + currency.type);
        }

        Identifier id;
        try {
            id = Identifier.of(currency.itemId);
        } catch (Exception exception) {
            return Result.failure("ID de item invalido: " + currency.itemId);
        }

        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR) {
            return Result.failure("No existe el item de moneda: " + currency.itemId);
        }

        int available = player.getInventory().count(item);
        if (available < currency.amount) {
            return Result.failure("Necesitas " + currency.amount + "x " + currency.itemId + " (tienes " + available + ")");
        }

        int remaining = currency.amount;
        for (int slot = 0; slot < player.getInventory().size() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isOf(item)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.decrement(take);
            remaining -= take;
        }
        player.getInventory().markDirty();
        return Result.success(item, currency.amount);
    }

    public void refund(ServerPlayerEntity player, Result withdrawal) {
        if (withdrawal == null || !withdrawal.success || withdrawal.item == null || withdrawal.amount <= 0) return;
        ItemStack refund = new ItemStack(withdrawal.item, withdrawal.amount);
        player.getInventory().insertStack(refund);
        if (!refund.isEmpty()) {
            player.dropItem(refund, false);
        }
        player.getInventory().markDirty();
    }

    public static final class Result {
        private final boolean success;
        private final String error;
        private final Item item;
        private final int amount;

        private Result(boolean success, String error, Item item, int amount) {
            this.success = success;
            this.error = error;
            this.item = item;
            this.amount = amount;
        }

        public static Result success(Item item, int amount) {
            return new Result(true, null, item, amount);
        }

        public static Result failure(String error) {
            return new Result(false, error, null, 0);
        }

        public boolean success() {
            return success;
        }

        public String error() {
            return error;
        }
    }
}
