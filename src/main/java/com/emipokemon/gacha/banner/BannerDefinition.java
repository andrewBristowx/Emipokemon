package com.emipokemon.gacha.banner;

import com.emipokemon.gacha.GachaTier;
import com.emipokemon.gacha.catalog.PokemonCatalogEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BannerDefinition {
    public int schemaVersion = 1;
    public String id = "standard";
    public String displayName = "Standard Banner";
    public boolean enabled = true;

    public List<Integer> generations = new ArrayList<>();
    public List<String> regions = new ArrayList<>();
    public List<String> types = new ArrayList<>();
    public List<String> requiredLabels = new ArrayList<>();
    public List<String> excludedLabels = new ArrayList<>();
    public List<String> excludedSpecies = new ArrayList<>();
    public List<String> allowedTiers = new ArrayList<>();

    public Map<String, Double> tierWeights = new HashMap<>();
    public Map<String, Double> featuredSpecies = new HashMap<>();
    public Map<String, Integer> minLevelByTier = new HashMap<>();
    public Map<String, Integer> maxLevelByTier = new HashMap<>();
    public Map<String, Double> shinyChanceByTier = new HashMap<>();

    public Currency currency = new Currency();
    public Pity pity = new Pity();

    public void normalize() {
        if (id == null || id.isBlank()) id = "standard";
        id = id.toLowerCase(Locale.ROOT).replace(' ', '_');
        if (displayName == null || displayName.isBlank()) displayName = id;
        if (generations == null) generations = new ArrayList<>();
        if (regions == null) regions = new ArrayList<>();
        if (types == null) types = new ArrayList<>();
        if (requiredLabels == null) requiredLabels = new ArrayList<>();
        if (excludedLabels == null) excludedLabels = new ArrayList<>();
        if (excludedSpecies == null) excludedSpecies = new ArrayList<>();
        if (allowedTiers == null) allowedTiers = new ArrayList<>();
        if (tierWeights == null) tierWeights = new HashMap<>();
        if (featuredSpecies == null) featuredSpecies = new HashMap<>();
        if (minLevelByTier == null) minLevelByTier = new HashMap<>();
        if (maxLevelByTier == null) maxLevelByTier = new HashMap<>();
        if (shinyChanceByTier == null) shinyChanceByTier = new HashMap<>();
        if (currency == null) currency = new Currency();
        if (pity == null) pity = new Pity();

        putWeight(GachaTier.COMMON, 45.0);
        putWeight(GachaTier.UNCOMMON, 27.0);
        putWeight(GachaTier.RARE, 15.0);
        putWeight(GachaTier.EPIC, 8.0);
        putWeight(GachaTier.LEGENDARY, 4.0);
        putWeight(GachaTier.MYTHICAL, 1.0);

        for (GachaTier tier : GachaTier.values()) {
            if (tier == GachaTier.SPECIAL) continue;
            minLevelByTier.putIfAbsent(tier.name(), switch (tier) {
                case COMMON -> 5;
                case UNCOMMON -> 10;
                case RARE -> 15;
                case EPIC -> 25;
                case LEGENDARY -> 50;
                case MYTHICAL, SPECIAL -> 50;
            });
            maxLevelByTier.putIfAbsent(tier.name(), switch (tier) {
                case COMMON -> 15;
                case UNCOMMON -> 20;
                case RARE -> 30;
                case EPIC -> 40;
                case LEGENDARY, MYTHICAL, SPECIAL -> 70;
            });
            shinyChanceByTier.putIfAbsent(tier.name(), 1.0 / 4096.0);
        }
        currency.normalize();
        pity.normalize();
    }

    private void putWeight(GachaTier tier, double value) {
        tierWeights.putIfAbsent(tier.name(), value);
    }

    public double weightFor(GachaTier tier) {
        return Math.max(0.0, tierWeights.getOrDefault(tier.name(), 0.0));
    }

    public int minLevelFor(GachaTier tier) {
        return Math.max(1, minLevelByTier.getOrDefault(tier.name(), 5));
    }

    public int maxLevelFor(GachaTier tier) {
        return Math.max(minLevelFor(tier), maxLevelByTier.getOrDefault(tier.name(), minLevelFor(tier)));
    }

    public double shinyChanceFor(GachaTier tier) {
        return Math.max(0.0, Math.min(1.0, shinyChanceByTier.getOrDefault(tier.name(), 1.0 / 4096.0)));
    }

    public double featuredMultiplier(String speciesId) {
        if (speciesId == null) return 1.0;
        String wanted = normalizeSpeciesKey(speciesId);
        double multiplier = 1.0;
        for (Map.Entry<String, Double> entry : featuredSpecies.entrySet()) {
            if (normalizeSpeciesKey(entry.getKey()).equals(wanted)) {
                multiplier = Math.max(multiplier, entry.getValue());
            }
        }
        return Math.max(1.0, multiplier);
    }

    public boolean allows(PokemonCatalogEntry entry) {
        if (!enabled) return false;
        if (!generations.isEmpty() && !generations.contains(entry.generation())) return false;
        if (!regions.isEmpty() && regions.stream().map(String::toLowerCase).noneMatch(entry.region()::equals)) return false;
        if (!types.isEmpty() && types.stream().map(String::toLowerCase).noneMatch(entry.types()::contains)) return false;
        if (!requiredLabels.isEmpty() && requiredLabels.stream().map(String::toLowerCase).anyMatch(label -> !entry.labels().contains(label))) return false;
        if (!excludedLabels.isEmpty() && excludedLabels.stream().map(String::toLowerCase).anyMatch(entry.labels()::contains)) return false;
        if (isExcluded(entry.speciesId())) return false;
        return allowedTiers.isEmpty() || allowedTiers.stream().anyMatch(value -> GachaTier.parse(value, GachaTier.SPECIAL) == entry.tier());
    }

    private boolean isExcluded(String speciesId) {
        String wanted = normalizeSpeciesKey(speciesId);
        return excludedSpecies.stream().anyMatch(value -> normalizeSpeciesKey(value).equals(wanted));
    }

    private String normalizeSpeciesKey(String value) {
        String key = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("cobblemon:")) key = key.substring("cobblemon:".length());
        return key;
    }

    public static final class Currency {
        public String type = "FREE";
        public String itemId = "minecraft:emerald";
        public int amount = 1;

        public void normalize() {
            if (type == null) type = "FREE";
            type = type.toUpperCase(Locale.ROOT);
            if (itemId == null || itemId.isBlank()) itemId = "minecraft:emerald";
            amount = Math.max(0, amount);
        }
    }

    public static final class Pity {
        public int epicGuarantee = 10;
        public int softLegendaryStart = 60;
        public double softLegendaryBonusPerPull = 0.5;
        public int hardLegendaryGuarantee = 90;

        public void normalize() {
            epicGuarantee = Math.max(0, epicGuarantee);
            softLegendaryStart = Math.max(0, softLegendaryStart);
            softLegendaryBonusPerPull = Math.max(0.0, softLegendaryBonusPerPull);
            hardLegendaryGuarantee = Math.max(0, hardLegendaryGuarantee);
        }
    }
}
