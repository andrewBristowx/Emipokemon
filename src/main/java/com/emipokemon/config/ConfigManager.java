package com.emipokemon.config;

import com.emipokemon.Emipokemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve(Emipokemon.MOD_ID);
    private final Path configFile = configDirectory.resolve("config.json");
    private volatile EmipokemonConfig config = new EmipokemonConfig();

    public synchronized void initialize() {
        try {
            Files.createDirectories(configDirectory);
            if (Files.notExists(configFile)) {
                config = new EmipokemonConfig();
                save();
                Emipokemon.LOGGER.info("Created default config at {}", configFile);
                return;
            }
            reload();
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not initialize Emipokemon config. Using safe defaults.", exception);
            config = new EmipokemonConfig();
        }
    }

    public synchronized boolean reload() {
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            EmipokemonConfig loaded = GSON.fromJson(reader, EmipokemonConfig.class);
            if (loaded == null) {
                throw new IOException("Config file is empty");
            }
            loaded.normalize();
            config = loaded;
            Emipokemon.LOGGER.info("Reloaded Emipokemon config version {}", loaded.configVersion);
            return true;
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Config reload failed; keeping the last known-good configuration", exception);
            return false;
        }
    }

    public synchronized void save() throws IOException {
        Files.createDirectories(configDirectory);
        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        }
    }

    public EmipokemonConfig get() {
        return config;
    }

    public Path configDirectory() {
        return configDirectory;
    }
}
