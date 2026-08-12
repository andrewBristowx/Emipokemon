package com.emipokemon.hologram;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HologramPersistenceTest {
    @Test
    void persistentRegistryDrivesVanillaTextDisplayRuntime() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/emipokemon/hologram/HologramService.java"));
        String vanilla = Files.readString(Path.of("src/main/java/com/emipokemon/hologram/VanillaTextHologram.java"));
        String registry = Files.readString(Path.of("src/main/java/com/emipokemon/hologram/HologramRegistryStore.java"));

        assertTrue(registry.contains("holograms-v1.properties"));
        assertTrue(service.contains("EntityType.TEXT_DISPLAY"));
        assertTrue(service.contains("migrateLegacyLoaded"));
        assertFalse(service.contains("ModRegistries.HOLOGRAM.create(world)"));
        assertTrue(vanilla.contains("EntityType.TEXT_DISPLAY.create(world)"));
        assertTrue(vanilla.contains("nbt.putString(\"billboard\", \"center\")"));
        assertTrue(vanilla.contains("Text.Serialization.toJsonString"));
        assertTrue(vanilla.contains("brightness.putInt(\"block\", 15)"));
        assertTrue(vanilla.contains("brightness.putInt(\"sky\", 15)"));
    }
}
