package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha54CompositionRegressionTest {
    private static String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void wheelHasNoSecondStaticCenterLogo() throws Exception {
        String source = screen();
        assertTrue(source.contains("drawAsset(context, ROULETTE_ALPHA51_WHEEL"));
        assertFalse(source.contains("drawAsset(context, ROULETTE_MEDALLION"));
    }

    @Test
    void variableTextUsesReservedImageLanes() throws Exception {
        String source = screen();
        assertTrue(source.contains("refX(1262)"));
        assertTrue(source.contains("refX(1194)"));
        assertTrue(source.contains("refX(1424)"));
        assertTrue(source.contains("refY(600)"));
        assertTrue(source.contains("refX(1418)"));
        assertTrue(source.contains("refX(218)"));
    }
}
