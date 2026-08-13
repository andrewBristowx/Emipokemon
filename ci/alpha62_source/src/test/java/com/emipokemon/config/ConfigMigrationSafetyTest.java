package com.emipokemon.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigrationSafetyTest {
    @Test
    void normalizedLegacyConfigIsPersistedWithoutReplacingExistingSections() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/emipokemon/config/ConfigManager.java"));
        int normalize = source.indexOf("loaded.normalize()");
        int assign = source.indexOf("config = loaded", normalize);
        int save = source.indexOf("save()", assign);
        assertTrue(normalize >= 0 && normalize < assign && assign < save);
        assertTrue(source.contains("previousVersion < loaded.configVersion || balanceWasMissing || casinoWasMissing"));
        assertTrue(source.contains("StandardCopyOption.ATOMIC_MOVE"));
    }
}
