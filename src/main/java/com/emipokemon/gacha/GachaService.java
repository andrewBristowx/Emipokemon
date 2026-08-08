package com.emipokemon.gacha;

import com.emipokemon.data.PlayerData;
import com.emipokemon.data.PlayerDataManager;
import com.emipokemon.gacha.banner.BannerDefinition;
import com.emipokemon.gacha.banner.BannerManager;
import com.emipokemon.gacha.catalog.PokemonCatalogEntry;
import com.emipokemon.gacha.catalog.PokemonCatalogService;
import com.emipokemon.gacha.economy.GachaCurrencyService;
import com.emipokemon.gacha.reward.CobblemonRewardService;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class GachaService {
    private final PokemonCatalogService catalog;
    private final BannerManager banners;
    private final PlayerDataManager playerData;
    private final GachaCurrencyService currency = new GachaCurrencyService();
    private final CobblemonRewardService rewards = new CobblemonRewardService();
    private final Map<UUID, Object> playerLocks = new ConcurrentHashMap<>();

    public GachaService(PokemonCatalogService catalog, BannerManager banners, PlayerDataManager playerData) {
        this.catalog = catalog;
        this.banners = banners;
        this.playerData = playerData;
    }

    public PullOutcome pull(ServerPlayerEntity player, String bannerId) {
        return pullInternal(player, bannerId, null);
    }

    public PullOutcome pullWithCurrencyOverride(
            ServerPlayerEntity player,
            String bannerId,
            BannerDefinition.Currency currencyOverride
    ) {
        return pullInternal(player, bannerId, currencyOverride);
    }

    private PullOutcome pullInternal(
            ServerPlayerEntity player,
            String bannerId,
            BannerDefinition.Currency currencyOverride
    ) {
        BannerDefinition banner = banners.get(bannerId);
        if (banner == null || !banner.enabled) return PullOutcome.failure("Banner no encontrado o desactivado: " + bannerId);
        if (catalog.size() == 0) return PullOutcome.failure("El catalogo Pokemon aun no esta disponible.");

        Object lock = playerLocks.computeIfAbsent(player.getUuid(), ignored -> new Object());
        synchronized (lock) {
            PlayerData data = playerData.getOrLoad(player.getUuid());
            GachaProgress progress = data.gacha(banner.id);
            GachaRollResult result = roll(banner, progress);
            if (result == null) return PullOutcome.failure("El banner no tiene Pokemon validos para sus filtros/tiers.");

            BannerDefinition.Currency effectiveCurrency = currencyOverride == null ? banner.currency : currencyOverride;
            GachaCurrencyService.Result withdrawal = currency.withdraw(player, effectiveCurrency);
            if (!withdrawal.success()) return PullOutcome.failure(withdrawal.error());

            if (!rewards.deliver(player, result)) {
                currency.refund(player, withdrawal);
                return PullOutcome.failure("No se pudo entregar el Pokemon; la moneda fue devuelta.");
            }

            progress.record(result.tier(), result.pokemon().speciesId());
            playerData.saveNow(player.getUuid());
            return PullOutcome.success(result);
        }
    }

    public GachaRollResult simulate(String bannerId) {
        BannerDefinition banner = banners.get(bannerId);
        if (banner == null || !banner.enabled || catalog.size() == 0) return null;
        return roll(banner, new GachaProgress());
    }

    public Map<GachaTier, Integer> poolCounts(String bannerId) {
        BannerDefinition banner = banners.get(bannerId);
        if (banner == null) return Map.of();
        Map<GachaTier, List<PokemonCatalogEntry>> pools = poolsFor(banner);
        Map<GachaTier, Integer> counts = new EnumMap<>(GachaTier.class);
        for (GachaTier tier : GachaTier.values()) counts.put(tier, pools.getOrDefault(tier, List.of()).size());
        return counts;
    }

    public List<PokemonCatalogEntry> pool(String bannerId, GachaTier tier) {
        BannerDefinition banner = banners.get(bannerId);
        if (banner == null) return List.of();
        return List.copyOf(poolsFor(banner).getOrDefault(tier, List.of()));
    }

    private GachaRollResult roll(BannerDefinition banner, GachaProgress progress) {
        Map<GachaTier, List<PokemonCatalogEntry>> pools = poolsFor(banner);
        GachaTier tier = rollTier(banner, progress, pools);
        if (tier == null) return null;

        List<PokemonCatalogEntry> candidates = pools.getOrDefault(tier, List.of());
        if (candidates.isEmpty()) return null;
        PokemonCatalogEntry selected = weightedSpecies(candidates, banner);
        int minLevel = banner.minLevelFor(tier);
        int maxLevel = banner.maxLevelFor(tier);
        int level = ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
        boolean shiny = ThreadLocalRandom.current().nextDouble() < banner.shinyChanceFor(tier);

        boolean epicPity = banner.pity.epicGuarantee > 0
                && progress.pullsSinceEpicOrBetter + 1 >= banner.pity.epicGuarantee
                && tier.isAtLeast(GachaTier.EPIC);
        boolean legendaryPity = banner.pity.hardLegendaryGuarantee > 0
                && progress.pullsSinceLegendaryOrBetter + 1 >= banner.pity.hardLegendaryGuarantee
                && tier.isAtLeast(GachaTier.LEGENDARY);

        return new GachaRollResult(banner.id, selected, tier, level, shiny, epicPity, legendaryPity);
    }

    private Map<GachaTier, List<PokemonCatalogEntry>> poolsFor(BannerDefinition banner) {
        Map<GachaTier, List<PokemonCatalogEntry>> pools = new EnumMap<>(GachaTier.class);
        for (GachaTier tier : GachaTier.values()) pools.put(tier, new ArrayList<>());
        for (PokemonCatalogEntry entry : catalog.values()) {
            if (banner.allows(entry)) pools.get(entry.tier()).add(entry);
        }
        return pools;
    }

    private GachaTier rollTier(BannerDefinition banner, GachaProgress progress, Map<GachaTier, List<PokemonCatalogEntry>> pools) {
        Map<GachaTier, Double> weights = new EnumMap<>(GachaTier.class);
        for (GachaTier tier : GachaTier.values()) {
            if (!pools.getOrDefault(tier, List.of()).isEmpty() && banner.weightFor(tier) > 0) {
                weights.put(tier, banner.weightFor(tier));
            }
        }
        if (weights.isEmpty()) return null;

        boolean hardLegendary = banner.pity.hardLegendaryGuarantee > 0
                && progress.pullsSinceLegendaryOrBetter + 1 >= banner.pity.hardLegendaryGuarantee;
        boolean epicGuarantee = banner.pity.epicGuarantee > 0
                && progress.pullsSinceEpicOrBetter + 1 >= banner.pity.epicGuarantee;

        if (hardLegendary && weights.keySet().stream().anyMatch(tier -> tier.isAtLeast(GachaTier.LEGENDARY))) {
            weights.entrySet().removeIf(entry -> !entry.getKey().isAtLeast(GachaTier.LEGENDARY));
        } else if (epicGuarantee && weights.keySet().stream().anyMatch(tier -> tier.isAtLeast(GachaTier.EPIC))) {
            weights.entrySet().removeIf(entry -> !entry.getKey().isAtLeast(GachaTier.EPIC));
        } else if (banner.pity.softLegendaryStart > 0
                && progress.pullsSinceLegendaryOrBetter + 1 >= banner.pity.softLegendaryStart) {
            int steps = progress.pullsSinceLegendaryOrBetter + 2 - banner.pity.softLegendaryStart;
            double bonus = Math.max(0, steps) * banner.pity.softLegendaryBonusPerPull;
            if (weights.containsKey(GachaTier.LEGENDARY)) {
                weights.computeIfPresent(GachaTier.LEGENDARY, (tier, weight) -> weight + bonus);
            }
        }

        return weightedTier(weights);
    }

    private GachaTier weightedTier(Map<GachaTier, Double> weights) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return null;
        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cursor = 0;
        for (Map.Entry<GachaTier, Double> entry : weights.entrySet()) {
            cursor += entry.getValue();
            if (roll < cursor) return entry.getKey();
        }
        return weights.keySet().iterator().next();
    }

    private PokemonCatalogEntry weightedSpecies(List<PokemonCatalogEntry> candidates, BannerDefinition banner) {
        Map<PokemonCatalogEntry, Double> weights = new HashMap<>();
        double total = 0;
        for (PokemonCatalogEntry entry : candidates) {
            double naturalWeight = 0.25 + Math.min(2.75, Math.max(0, entry.catchRate()) / 60.0);
            double weight = naturalWeight * banner.featuredMultiplier(entry.speciesId());
            weights.put(entry, weight);
            total += weight;
        }

        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cursor = 0;
        for (PokemonCatalogEntry entry : candidates) {
            cursor += weights.get(entry);
            if (roll < cursor) return entry;
        }
        return candidates.get(candidates.size() - 1);
    }

    public record PullOutcome(boolean success, String error, GachaRollResult result) {
        public static PullOutcome success(GachaRollResult result) {
            return new PullOutcome(true, null, result);
        }

        public static PullOutcome failure(String error) {
            return new PullOutcome(false, error, null);
        }
    }
}
