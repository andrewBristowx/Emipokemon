package com.emipokemon.rewards;

import com.emipokemon.data.PlayerData;
import com.emipokemon.data.PlayerDataManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;
import java.util.UUID;

/** Atomic persisted credits granted by daily rewards and the infinite pass. */
public final class RewardWalletService {
    public static final String STANDARD = "VIRTUAL_STANDARD";
    public static final String EMI = "VIRTUAL_EMI";

    private final PlayerDataManager dataManager;

    public RewardWalletService(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public synchronized long balance(UUID playerId, String type) {
        PlayerData data = dataManager.getOrLoad(playerId);
        return EMI.equals(normalize(type)) ? data.rewardWallet.emiRolls : data.rewardWallet.standardRolls;
    }

    public long balance(ServerPlayerEntity player, String type) {
        return balance(player.getUuid(), type);
    }

    public synchronized boolean withdraw(ServerPlayerEntity player, String type, long amount) {
        if (player == null || amount <= 0L) return false;
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        String normalized = normalize(type);
        long before = normalized.equals(EMI) ? data.rewardWallet.emiRolls : data.rewardWallet.standardRolls;
        if (before < amount) return false;
        if (normalized.equals(EMI)) data.rewardWallet.emiRolls -= amount;
        else data.rewardWallet.standardRolls -= amount;
        if (dataManager.saveNowChecked(player.getUuid())) return true;
        if (normalized.equals(EMI)) data.rewardWallet.emiRolls = before;
        else data.rewardWallet.standardRolls = before;
        return false;
    }

    public synchronized boolean credit(ServerPlayerEntity player, String type, long amount) {
        if (player == null || amount <= 0L) return false;
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        String normalized = normalize(type);
        long before = normalized.equals(EMI) ? data.rewardWallet.emiRolls : data.rewardWallet.standardRolls;
        long after;
        try { after = Math.addExact(before, amount); }
        catch (ArithmeticException overflow) { after = Long.MAX_VALUE; }
        if (normalized.equals(EMI)) data.rewardWallet.emiRolls = after;
        else data.rewardWallet.standardRolls = after;
        if (dataManager.saveNowChecked(player.getUuid())) return true;
        if (normalized.equals(EMI)) data.rewardWallet.emiRolls = before;
        else data.rewardWallet.standardRolls = before;
        return false;
    }

    public void refund(ServerPlayerEntity player, String type, long amount) {
        credit(player, type, amount);
    }

    private String normalize(String type) {
        String value = type == null ? "" : type.toUpperCase(Locale.ROOT);
        return value.contains("EMI") ? EMI : STANDARD;
    }
}
