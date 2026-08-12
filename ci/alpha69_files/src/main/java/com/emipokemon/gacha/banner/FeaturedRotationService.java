package com.emipokemon.gacha.banner;

import com.emipokemon.Emipokemon;
import com.emipokemon.gacha.GachaTier;
import com.emipokemon.gacha.catalog.PokemonCatalogEntry;
import com.emipokemon.gacha.catalog.PokemonCatalogService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Server-authoritative featured rotation used by the Emi gacha.
 * A single legendary is selected from every implemented LEGENDARY catalog entry
 * and remains stable for a real-world twelve-hour window, including restarts.
 */
public final class FeaturedRotationService {
    public static final long EMI_ROTATION_MILLIS = 12L * 60L * 60L * 1000L;
    public static final double EMI_FEATURED_MULTIPLIER = 6.0D;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final PokemonCatalogService catalog;
    private final Path stateFile = FabricLoader.getInstance().getConfigDir()
            .resolve(Emipokemon.MOD_ID).resolve("gacha").resolve("emi_featured_rotation.json");
    private RotationState state;
    private boolean loaded;

    public FeaturedRotationService(PokemonCatalogService catalog) {
        this.catalog = catalog;
    }

    /** Returns the current Emi legendary, or null until the Cobblemon catalog is ready. */
    public synchronized PokemonCatalogEntry currentEmiLegendary() {
        return currentEmiLegendary(System.currentTimeMillis());
    }

    synchronized PokemonCatalogEntry currentEmiLegendary(long epochMillis) {
        List<PokemonCatalogEntry> pool = legendaryPool();
        if (pool.isEmpty()) return null;
        ensureLoaded();

        long bucket = rotationBucket(epochMillis);
        PokemonCatalogEntry selected = state == null ? null : catalog.get(state.speciesId);
        boolean selectedStillValid = selected != null && selected.tier() == GachaTier.LEGENDARY;
        if (state == null || state.bucket != bucket || !selectedStillValid) {
            String previous = state == null ? "" : state.speciesId;
            ArrayList<PokemonCatalogEntry> choices = new ArrayList<>(pool);
            if (choices.size() > 1 && previous != null && !previous.isBlank()) {
                choices.removeIf(entry -> entry.speciesId().equalsIgnoreCase(previous));
            }
            PokemonCatalogEntry next = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
            if (state == null) state = new RotationState();
            state.bucket = bucket;
            state.speciesId = next.speciesId();
            saveState();
            Emipokemon.LOGGER.info("Emi gacha featured legendary for rotation {} is {}",
                    bucket, next.displayName());
            return next;
        }
        return selected;
    }

    /** Random visual spotlight for the normal machine. It never changes normal gacha weights. */
    public synchronized PokemonCatalogEntry currentStandardSpotlight(BannerDefinition banner) {
        List<PokemonCatalogEntry> pool = catalog.all().stream()
                .filter(entry -> banner == null || banner.allows(entry))
                .toList();
        if (pool.isEmpty()) return null;
        ensureLoaded();
        long bucket = rotationBucket(System.currentTimeMillis());
        PokemonCatalogEntry selected = state == null ? null : catalog.get(state.standardSpeciesId);
        boolean valid = selected != null && (banner == null || banner.allows(selected));
        if (state == null) state = new RotationState();
        if (state.standardBucket != bucket || !valid) {
            String previous = state.standardSpeciesId == null ? "" : state.standardSpeciesId;
            ArrayList<PokemonCatalogEntry> choices = new ArrayList<>(pool);
            if (choices.size() > 1 && !previous.isBlank()) {
                choices.removeIf(entry -> entry.speciesId().equalsIgnoreCase(previous));
            }
            selected = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
            state.standardBucket = bucket;
            state.standardSpeciesId = selected.speciesId();
            saveState();
        }
        return selected;
    }

    /** The complete implemented LEGENDARY pool, sorted for stable admin/debug output. */
    public synchronized List<PokemonCatalogEntry> legendaryPool() {
        return catalog.all().stream()
                .filter(entry -> entry.tier() == GachaTier.LEGENDARY)
                .sorted(Comparator.comparingInt(PokemonCatalogEntry::nationalDex))
                .toList();
    }

    public static long rotationBucket(long epochMillis) {
        return Math.floorDiv(Math.max(0L, epochMillis), EMI_ROTATION_MILLIS);
    }

    private void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (Files.notExists(stateFile)) return;
        try (Reader reader = Files.newBufferedReader(stateFile, StandardCharsets.UTF_8)) {
            state = GSON.fromJson(reader, RotationState.class);
        } catch (Exception exception) {
            state = null;
            Emipokemon.LOGGER.warn("Could not load Emi featured rotation; a new legendary will be selected", exception);
        }
    }

    private void saveState() {
        try {
            Files.createDirectories(stateFile.getParent());
            try (Writer writer = Files.newBufferedWriter(stateFile, StandardCharsets.UTF_8)) {
                GSON.toJson(state, writer);
            }
        } catch (Exception exception) {
            Emipokemon.LOGGER.warn("Could not persist Emi featured rotation", exception);
        }
    }

    private static final class RotationState {
        long bucket = -1L;
        String speciesId = "";
        long standardBucket = -1L;
        String standardSpeciesId = "";
    }
}
