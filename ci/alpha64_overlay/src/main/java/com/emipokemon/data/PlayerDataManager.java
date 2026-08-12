package com.emipokemon.data;

import com.emipokemon.Emipokemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Path dataDirectory = FabricLoader.getInstance().getConfigDir()
            .resolve(Emipokemon.MOD_ID)
            .resolve("players");

    public PlayerData getOrLoad(UUID playerId) {
        return cache.computeIfAbsent(playerId, this::readOrCreate);
    }

    public void load(UUID playerId) {
        PlayerData data = getOrLoad(playerId);
        data.normalize();
        data.touch();
    }

    public void saveNow(UUID playerId) {
        saveNowChecked(playerId);
    }

    public boolean saveNowChecked(UUID playerId) {
        PlayerData data = cache.get(playerId);
        if (data != null) {
            data.normalize();
            data.touch();
            return writeSafely(data);
        }
        return false;
    }

    public void saveAndUnload(UUID playerId) {
        PlayerData data = cache.remove(playerId);
        if (data != null) {
            data.normalize();
            data.touch();
            writeSafely(data);
        }
    }

    public void saveAll() {
        cache.values().forEach(data -> {
            data.normalize();
            data.touch();
            writeSafely(data);
        });
    }

    public int loadedCount() {
        return cache.size();
    }

    private PlayerData readOrCreate(UUID playerId) {
        Path file = fileFor(playerId);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                PlayerData data = GSON.fromJson(reader, PlayerData.class);
                if (data != null) {
                    data.playerId = playerId;
                    data.normalize();
                    return data;
                }
            } catch (Exception exception) {
                Emipokemon.LOGGER.error("Could not read player data for {}. A fresh in-memory record will be used.", playerId, exception);
            }
        }
        return PlayerData.create(playerId);
    }

    private boolean writeSafely(PlayerData data) {
        try {
            Files.createDirectories(dataDirectory);
            Path target = fileFor(data.playerId);
            Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicMoveFailure) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not persist player data for {}", data.playerId, exception);
            return false;
        }
    }

    private Path fileFor(UUID playerId) {
        return dataDirectory.resolve(playerId + ".json");
    }
}
