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
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

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
        try {
            EmipokemonConfig loaded;
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, EmipokemonConfig.class);
            }
            if (loaded == null) {
                throw new IOException("Config file is empty");
            }
            int previousVersion = loaded.configVersion;
            boolean balanceWasMissing = loaded.balance == null;
            boolean casinoWasMissing = loaded.casino == null;
            loaded.normalize();
            config = loaded;
            if (previousVersion < loaded.configVersion || balanceWasMissing || casinoWasMissing) {
                save();
                Emipokemon.LOGGER.info("Migrated Emipokemon config {} -> {}", previousVersion, loaded.configVersion);
            }
            Emipokemon.LOGGER.info("Reloaded Emipokemon config version {}", loaded.configVersion);
            return true;
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Config reload failed; keeping the last known-good configuration", exception);
            return false;
        }
    }

    public synchronized void save() throws IOException {
        Files.createDirectories(configDirectory);
        Path temporary = configFile.resolveSibling(configFile.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        }
        try {
            Files.move(temporary, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception atomicMoveFailure) {
            Files.move(temporary, configFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public synchronized boolean update(Consumer<EmipokemonConfig> mutation) {
        try {
            mutation.accept(config);
            config.normalize();
            save();
            return true;
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not persist Emipokemon configuration update", exception);
            return false;
        }
    }

    public EmipokemonConfig get() {
        return config;
    }

    public Path configDirectory() {
        return configDirectory;
    }
}
