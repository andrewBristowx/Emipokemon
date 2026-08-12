package com.emipokemon.rewards;

import com.emipokemon.Emipokemon;
import com.emipokemon.config.ConfigManager;
import com.emipokemon.config.EmipokemonConfig;
import com.emipokemon.data.PlayerData;
import com.emipokemon.data.PlayerDataManager;
import com.emipokemon.gacha.GachaTier;
import com.emipokemon.gacha.catalog.PokemonCatalogEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Infinite, server-authoritative two-track progression pass. */
public final class BattlePassService {
    public static final int PAGE_SIZE = 8;
    private final PlayerDataManager dataManager;
    private final ConfigManager configManager;
    private final Map<UUID, CaptureWindow> captureWindows = new ConcurrentHashMap<>();
    private final Path auditFile = FabricLoader.getInstance().getConfigDir()
            .resolve(Emipokemon.MOD_ID).resolve("battle-pass-audit.log");

    public BattlePassService(PlayerDataManager dataManager, ConfigManager configManager) {
        this.dataManager = dataManager;
        this.configManager = configManager;
    }

    public void playerLeft(UUID playerId) {
        captureWindows.remove(playerId);
    }

    public void onActiveSecond(ServerPlayerEntity player) {
        EmipokemonConfig.BattlePassSettings settings = settings();
        if (!settings.enabled || settings.activeRewardXp <= 0) return;
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        data.battlePass.activeSecondsBank++;
        if (data.battlePass.activeSecondsBank < settings.activeRewardSeconds) return;
        data.battlePass.activeSecondsBank -= settings.activeRewardSeconds;
        addXp(player, settings.activeRewardXp, "active_time");
    }

    public void onCapture(ServerPlayerEntity player, PokemonCatalogEntry entry, boolean newSpecies) {
        if (entry == null || !captureRateAllows(player.getUuid())) return;
        EmipokemonConfig.BattlePassSettings value = settings();
        int xp = switch (entry.tier()) {
            case COMMON -> value.commonCaptureXp;
            case UNCOMMON -> value.uncommonCaptureXp;
            case RARE -> value.rareCaptureXp;
            case EPIC -> value.epicCaptureXp;
            case LEGENDARY -> value.legendaryCaptureXp;
            case MYTHICAL, SPECIAL -> value.mythicalCaptureXp;
        };
        if (newSpecies) xp = Math.addExact(xp, value.newSpeciesBonusXp);
        addXp(player, xp, "capture:" + entry.tier().name().toLowerCase(java.util.Locale.ROOT));
    }

    public void onEvolution(ServerPlayerEntity player) { addXp(player, settings().evolutionXp, "evolution"); }
    public void onBattleVictory(ServerPlayerEntity player, boolean wild) {
        addXp(player, wild ? settings().wildBattleXp : settings().trainerBattleXp, wild ? "wild_battle" : "trainer_battle");
    }
    public void onNewBiome(ServerPlayerEntity player) { addXp(player, settings().newBiomeXp, "new_biome"); }
    public void onJobLevel(ServerPlayerEntity player, int levels) {
        addXp(player, (long) settings().jobLevelXp * Math.max(1, levels), "job_level");
    }
    public void onQuestClaim(ServerPlayerEntity player, long target) {
        EmipokemonConfig.BattlePassSettings value = settings();
        long scaled = value.questMinimumXp + Math.min((long) value.questMaximumXp - value.questMinimumXp,
                Math.max(0L, target) * 5L);
        addXp(player, scaled, "quest_claim");
    }

    public synchronized long addXp(ServerPlayerEntity player, long requested, String reason) {
        if (player == null || requested <= 0L || !settings().enabled) return 0L;
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        long before = data.battlePass.experience;
        int oldLevel = levelFor(before);
        long amount = Math.min(requested, Long.MAX_VALUE - before);
        data.battlePass.experience = before + amount;
        if (!dataManager.saveNowChecked(player.getUuid())) {
            data.battlePass.experience = before;
            return 0L;
        }
        int newLevel = levelFor(data.battlePass.experience);
        audit(player.getUuid(), "xp", amount + ":" + reason + ":level=" + newLevel);
        if (newLevel > oldLevel) {
            player.sendMessage(Text.literal("§d✦ Pase de Emi: alcanzaste el nivel §f" + newLevel
                    + "§d. Abre /pase para reclamar."), false);
        }
        return amount;
    }

    public synchronized boolean claim(ServerPlayerEntity player, boolean premiumTrack, int level) {
        if (player == null || level < 1 || level > levelFor(dataManager.getOrLoad(player.getUuid()).battlePass.experience)) return false;
        EmipokemonConfig.BattlePassSettings settings = settings();
        if (premiumTrack && !BattlePassAccessPolicy.hasPremium(player, settings)) return false;
        int amount = premiumTrack ? premiumReward(level, settings) : freeReward(level, settings);
        if (amount <= 0) return false;
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        java.util.Set<Integer> claimed = premiumTrack ? data.battlePass.claimedPremium : data.battlePass.claimedFree;
        if (!claimed.add(level)) return false;
        long before = data.rewardWallet.emiRolls;
        try { data.rewardWallet.emiRolls = Math.addExact(before, amount); }
        catch (ArithmeticException overflow) { data.rewardWallet.emiRolls = Long.MAX_VALUE; }
        if (!dataManager.saveNowChecked(player.getUuid())) {
            claimed.remove(level);
            data.rewardWallet.emiRolls = before;
            return false;
        }
        String track = premiumTrack ? "premium" : "free";
        audit(player.getUuid(), "claim", track + ":level=" + level + ":emi_rolls=" + amount);
        player.sendMessage(Text.literal("§dPase de Emi: +" + amount + " tirada" + (amount == 1 ? "" : "s")
                + " de Emi §7(" + track + " nivel " + level + ")"), false);
        return true;
    }

    public BattlePassSnapshot snapshot(ServerPlayerEntity player, int requestedPage, String message) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        int level = levelFor(data.battlePass.experience);
        int currentPage = Math.max(0, (level - 1) / PAGE_SIZE);
        int page = requestedPage < 0 ? currentPage : Math.max(0, requestedPage);
        BattlePassSnapshot snapshot = new BattlePassSnapshot();
        snapshot.playerName = player.getGameProfile().getName();
        snapshot.experience = data.battlePass.experience;
        snapshot.level = level;
        snapshot.levelStartXp = totalXpForLevel(level);
        snapshot.nextLevelXp = totalXpForLevel(level + 1);
        snapshot.premium = BattlePassAccessPolicy.hasPremium(player, settings());
        snapshot.page = page;
        snapshot.standardRolls = data.rewardWallet.standardRolls;
        snapshot.emiRolls = data.rewardWallet.emiRolls;
        snapshot.message = message == null ? "" : message;
        int first = page * PAGE_SIZE + 1;
        for (int displayed = 0; displayed < PAGE_SIZE; displayed++) {
            int target = first + displayed;
            snapshot.free.add(slot(target, level, false, true, data, settings()));
            snapshot.premiumTrack.add(slot(target, level, true, snapshot.premium, data, settings()));
        }
        return snapshot;
    }

    private BattlePassSnapshot.RewardSlot slot(int target, int current, boolean premium, boolean trackAvailable, PlayerData data,
                                                EmipokemonConfig.BattlePassSettings settings) {
        BattlePassSnapshot.RewardSlot slot = new BattlePassSnapshot.RewardSlot();
        slot.level = target;
        slot.amount = premium ? premiumReward(target, settings) : freeReward(target, settings);
        slot.type = slot.amount > 0 ? "EMI_ROLLS" : "MILESTONE";
        slot.label = slot.amount > 0 ? slot.amount + " tirada" + (slot.amount == 1 ? "" : "s") + " de Emi" : "Hito de progreso";
        slot.unlocked = current >= target;
        slot.claimed = (premium ? data.battlePass.claimedPremium : data.battlePass.claimedFree).contains(target);
        slot.claimable = slot.amount > 0 && slot.unlocked && !slot.claimed && trackAvailable;
        return slot;
    }

    public int levelFor(long experience) {
        if (experience <= 0L) return 1;
        int low = 1;
        int high = 2;
        while (high < 1_000_000_000 && totalXpForLevel(high) <= experience) high = Math.min(1_000_000_000, high * 2);
        while (low + 1 < high) {
            int middle = low + (high - low) / 2;
            if (totalXpForLevel(middle) <= experience) low = middle;
            else high = middle;
        }
        return low;
    }

    public long totalXpForLevel(int level) {
        if (level <= 1) return 0L;
        EmipokemonConfig.BattlePassSettings value = settings();
        long transitions = (long) level - 1L;
        long growingTransitions = value.xpGrowthPerLevel == 0 ? 0L
                : Math.min(transitions, Math.max(0L, ((long) value.maximumXpPerLevel - value.baseXpPerLevel
                + value.xpGrowthPerLevel - 1L) / value.xpGrowthPerLevel));
        long total = saturatingAdd(saturatingMultiply(growingTransitions, value.baseXpPerLevel),
                saturatingMultiply(value.xpGrowthPerLevel,
                        growingTransitions * Math.max(0L, growingTransitions - 1L) / 2L));
        long cappedTransitions = transitions - growingTransitions;
        long capped = saturatingMultiply(cappedTransitions, value.maximumXpPerLevel);
        return saturatingAdd(total, capped);
    }

    private int freeReward(int level, EmipokemonConfig.BattlePassSettings value) {
        return level % value.freeRewardEveryLevels == 0 ? value.freeEmiRolls : 0;
    }

    private int premiumReward(int level, EmipokemonConfig.BattlePassSettings value) {
        if (level == 1) return value.premiumFirstLevelEmiRolls;
        return level % value.premiumRewardEveryLevels == 0 ? value.premiumEmiRolls : 0;
    }

    private boolean captureRateAllows(UUID playerId) {
        long minute = System.currentTimeMillis() / 60_000L;
        CaptureWindow window = captureWindows.computeIfAbsent(playerId, ignored -> new CaptureWindow());
        synchronized (window) {
            if (window.minute != minute) { window.minute = minute; window.events = 0; }
            if (window.events >= settings().captureXpEventsPerMinute) return false;
            window.events++;
            return true;
        }
    }

    private long saturatingMultiply(long left, long right) {
        try { return Math.multiplyExact(left, right); }
        catch (ArithmeticException overflow) { return Long.MAX_VALUE; }
    }

    private long saturatingAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private EmipokemonConfig.BattlePassSettings settings() { return configManager.get().battlePass; }

    private void audit(UUID playerId, String action, String detail) {
        try {
            Files.createDirectories(auditFile.getParent());
            Files.writeString(auditFile, System.currentTimeMillis() + "\t" + playerId + "\t" + action + "\t" + detail
                            + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not write battle pass audit", exception);
        }
    }

    private static final class CaptureWindow { long minute; int events; }
}
