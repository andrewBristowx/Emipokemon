package com.emipokemon.gacha.economy;

import com.emipokemon.gacha.banner.BannerDefinition;
import com.emipokemon.rewards.RewardWalletService;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class GachaCurrencyService {
    private final RewardWalletService wallet;

    public GachaCurrencyService(RewardWalletService wallet) {
        this.wallet = wallet;
    }

    public Result withdraw(ServerPlayerEntity player, BannerDefinition.Currency currency) {
        if (currency == null || currency.amount <= 0 || "FREE".equalsIgnoreCase(currency.type)) {
            return Result.success((Item) null, 0);
        }
        if (!"ITEM".equalsIgnoreCase(currency.type)) {
            String type = currency.type == null ? "" : currency.type.toUpperCase(java.util.Locale.ROOT);
            if (RewardWalletService.EMI.equals(type) || RewardWalletService.STANDARD.equals(type)) {
                if (!wallet.withdraw(player, type, currency.amount)) {
                    return Result.failure(type.equals(RewardWalletService.EMI)
                            ? "No tienes tiradas de Emi guardadas."
                            : "No tienes tiradas estándar guardadas.");
                }
                return Result.success(type, currency.amount);
            }
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
        if (withdrawal == null || !withdrawal.success || withdrawal.amount <= 0) return;
        if (withdrawal.virtualType != null) {
            wallet.refund(player, withdrawal.virtualType, withdrawal.amount);
            return;
        }
        if (withdrawal.item == null) return;
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
        private final String virtualType;
        private final int amount;

        private Result(boolean success, String error, Item item, String virtualType, int amount) {
            this.success = success;
            this.error = error;
            this.item = item;
            this.virtualType = virtualType;
            this.amount = amount;
        }

        public static Result success(Item item, int amount) {
            return new Result(true, null, item, null, amount);
        }

        public static Result success(String virtualType, int amount) {
            return new Result(true, null, null, virtualType, amount);
        }

        public static Result failure(String error) {
            return new Result(false, error, null, null, 0);
        }

        public boolean success() {
            return success;
        }

        public String error() {
            return error;
        }
    }
}
