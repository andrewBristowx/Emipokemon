package com.emipokemon.gacha.catalog;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.emipokemon.Emipokemon;
import com.emipokemon.gacha.GachaTier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PokemonCatalogService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type OVERRIDE_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final Path overridesFile = FabricLoader.getInstance().getConfigDir()
            .resolve(Emipokemon.MOD_ID).resolve("gacha").resolve("catalog_overrides.json");
    private final Map<String, PokemonCatalogEntry> entries = new HashMap<>();
    private Map<String, String> overrides = new HashMap<>();

    public synchronized void rebuild() {
        loadOverrides();
        entries.clear();
        for (Species species : PokemonSpecies.getImplemented()) {
            PokemonCatalogEntry entry = fromSpecies(species);
            entries.put(entry.speciesId(), entry);
        }
        Emipokemon.LOGGER.info("Emipokemon gacha catalog built with {} implemented Pokemon", entries.size());
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized PokemonCatalogEntry get(String speciesId) {
        if (speciesId == null) return null;
        return entries.get(normalizeSpeciesId(speciesId));
    }

    public synchronized List<PokemonCatalogEntry> all() {
        return entries.values().stream()
                .sorted(Comparator.comparingInt(PokemonCatalogEntry::nationalDex))
                .toList();
    }

    public synchronized Collection<PokemonCatalogEntry> values() {
        return new ArrayList<>(entries.values());
    }

    public synchronized Map<GachaTier, Long> tierCounts() {
        Map<GachaTier, Long> result = new HashMap<>();
        for (GachaTier tier : GachaTier.values()) result.put(tier, 0L);
        for (PokemonCatalogEntry entry : entries.values()) {
            result.compute(entry.tier(), (key, value) -> value == null ? 1L : value + 1L);
        }
        return result;
    }

    private PokemonCatalogEntry fromSpecies(Species species) {
        // showdownId is mapping-neutral (String), unlike resourceIdentifier whose Minecraft
        // ResourceLocation type differs between Cobblemon's Mojang mappings and our Yarn workspace.
        String id = normalizeSpeciesId(species.showdownId());
        Set<String> labels = new LinkedHashSet<>();
        species.getLabels().forEach(label -> labels.add(label.toLowerCase(Locale.ROOT)));

        int generation = generationFrom(labels, species.getNationalPokedexNumber());
        Set<String> types = new LinkedHashSet<>();
        types.add(species.getPrimaryType().getName().toLowerCase(Locale.ROOT));
        if (species.getSecondaryType() != null) {
            types.add(species.getSecondaryType().getName().toLowerCase(Locale.ROOT));
        }

        int bst = species.getBaseStats().values().stream().mapToInt(Integer::intValue).sum();
        GachaTier tier = classify(labels, bst, species.getCatchRate());
        String override = overrides.get(id);
        if (override == null) override = overrides.get(species.getName().toLowerCase(Locale.ROOT));
        tier = GachaTier.parse(override, tier);

        return new PokemonCatalogEntry(
                id,
                species.getName(),
                species.getNationalPokedexNumber(),
                generation,
                regionFor(generation),
                types,
                labels,
                bst,
                species.getCatchRate(),
                tier
        );
    }

    private GachaTier classify(Set<String> labels, int bst, int catchRate) {
        if (labels.contains("mythical")) return GachaTier.MYTHICAL;
        if (labels.contains("legendary") || labels.contains("ultra_beast")) return GachaTier.LEGENDARY;
        if (labels.contains("paradox") || labels.contains("powerhouse") || labels.contains("starter")) return GachaTier.EPIC;
        if (bst >= 570 || catchRate <= 15) return GachaTier.EPIC;
        if (bst >= 500 || catchRate <= 45) return GachaTier.RARE;
        if (bst >= 420 || catchRate <= 90) return GachaTier.UNCOMMON;
        return GachaTier.COMMON;
    }

    private int generationFrom(Set<String> labels, int dex) {
        for (String label : labels) {
            if (label.startsWith("gen")) {
                try {
                    int generation = Integer.parseInt(label.substring(3));
                    if (generation >= 1 && generation <= 20) return generation;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (dex <= 151) return 1;
        if (dex <= 251) return 2;
        if (dex <= 386) return 3;
        if (dex <= 493) return 4;
        if (dex <= 649) return 5;
        if (dex <= 721) return 6;
        if (dex <= 809) return 7;
        if (dex <= 905) return 8;
        return 9;
    }

    private String regionFor(int generation) {
        return switch (generation) {
            case 1 -> "kanto";
            case 2 -> "johto";
            case 3 -> "hoenn";
            case 4 -> "sinnoh";
            case 5 -> "unova";
            case 6 -> "kalos";
            case 7 -> "alola";
            case 8 -> "galar";
            case 9 -> "paldea";
            default -> "unknown";
        };
    }

    private String normalizeSpeciesId(String speciesId) {
        String value = speciesId.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("cobblemon:")) value = value.substring("cobblemon:".length());
        return value;
    }

    private void loadOverrides() {
        try {
            Files.createDirectories(overridesFile.getParent());
            if (Files.notExists(overridesFile)) {
                overrides = new HashMap<>();
                try (Writer writer = Files.newBufferedWriter(overridesFile, StandardCharsets.UTF_8)) {
                    GSON.toJson(overrides, OVERRIDE_TYPE, writer);
                }
                return;
            }
            try (Reader reader = Files.newBufferedReader(overridesFile, StandardCharsets.UTF_8)) {
                Map<String, String> loaded = GSON.fromJson(reader, OVERRIDE_TYPE);
                overrides = loaded == null ? new HashMap<>() : loaded;
            }
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not load gacha catalog overrides; using automatic classification", exception);
            overrides = new HashMap<>();
        }
    }
}
