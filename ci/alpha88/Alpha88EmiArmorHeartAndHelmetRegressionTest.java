package com.emipokemon.alpha88;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class Alpha88EmiArmorHeartAndHelmetRegressionTest {
    @Test
    void versionAndArmorAssetsAreAlpha88() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.88"));
        String core = Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java"));
        assertTrue(core.contains("VERSION = \"0.4.0-alpha.88\""));
        String geo = Files.readString(Path.of("src/main/resources/assets/emipokemon/geo/armor/emi_armor.geo.json"));
        assertTrue(geo.contains("\"name\": \"helmet_heart\""));
        assertTrue(geo.contains("\"name\": \"heart_core\""));
    }
}
