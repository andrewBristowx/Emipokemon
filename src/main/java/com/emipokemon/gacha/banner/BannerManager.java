package com.emipokemon.gacha.banner;

import com.emipokemon.Emipokemon;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class BannerManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path bannerDirectory = FabricLoader.getInstance().getConfigDir()
            .resolve(Emipokemon.MOD_ID).resolve("gacha").resolve("banners");
    private volatile Map<String, BannerDefinition> banners = Map.of();

    public synchronized void initialize() {
        try {
            Files.createDirectories(bannerDirectory);
            createDefaultsIfMissing();
            reload();
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not initialize gacha banners", exception);
        }
    }

    public synchronized boolean reload() {
        try {
            Files.createDirectories(bannerDirectory);
            Map<String, BannerDefinition> loaded = new HashMap<>();
            try (Stream<Path> files = Files.list(bannerDirectory)) {
                for (Path path : files.filter(file -> file.getFileName().toString().endsWith(".json")).toList()) {
                    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                        BannerDefinition banner = GSON.fromJson(reader, BannerDefinition.class);
                        if (banner == null) throw new IllegalStateException("Empty banner file: " + path);
                        banner.normalize();
                        if (loaded.putIfAbsent(banner.id, banner) != null) {
                            throw new IllegalStateException("Duplicate banner id: " + banner.id);
                        }
                    }
                }
            }
            if (loaded.isEmpty()) throw new IllegalStateException("No gacha banners were loaded");
            banners = Map.copyOf(loaded);
            Emipokemon.LOGGER.info("Loaded {} Emipokemon gacha banners", banners.size());
            return true;
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Gacha banner reload failed; keeping last known-good banners", exception);
            return false;
        }
    }

    public BannerDefinition get(String id) {
        if (id == null) return null;
        return banners.get(id.toLowerCase(Locale.ROOT));
    }

    public List<BannerDefinition> all() {
        return new ArrayList<>(banners.values()).stream()
                .sorted(Comparator.comparing(banner -> banner.id))
                .toList();
    }

    public int size() {
        return banners.size();
    }

    public Path directory() {
        return bannerDirectory;
    }

    private void createDefaultsIfMissing() throws Exception {
        Path standard = bannerDirectory.resolve("standard.json");
        if (Files.notExists(standard)) {
            BannerDefinition banner = new BannerDefinition();
            banner.id = "standard";
            banner.displayName = "Gacha Estandar";
            banner.normalize();
            write(standard, banner);
        }

        Path rayquaza = bannerDirectory.resolve("rayquaza_hoenn.json");
        if (Files.notExists(rayquaza)) {
            BannerDefinition banner = new BannerDefinition();
            banner.id = "rayquaza_hoenn";
            banner.displayName = "Rayquaza - Cielos de Hoenn";
            banner.generations.add(3);
            banner.featuredSpecies.put("cobblemon:rayquaza", 6.0);
            banner.normalize();
            write(rayquaza, banner);
        }
    }

    private void write(Path path, BannerDefinition banner) throws Exception {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(banner, writer);
        }
    }
}
