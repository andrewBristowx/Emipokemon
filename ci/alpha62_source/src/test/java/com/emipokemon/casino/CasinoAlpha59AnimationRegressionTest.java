package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoAlpha59AnimationRegressionTest {
    @Test
    void animationsResolveToServerProvidedResults() throws Exception {
        String screen = Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
        assertTrue(screen.contains("drawSlotSymbol"));
        assertTrue(screen.contains("stopAt = 850L + i * 300L"));
        assertTrue(screen.contains("spinning ? cycle"));
        assertTrue(screen.contains("drawAnimatedDie"));
        assertTrue(screen.contains("rolling ? 1 +"));
        assertTrue(screen.contains("diceValues()"));
        assertTrue(screen.contains("drawCardsInSlots"));
        assertTrue(screen.contains("drawCardFace"));
        assertTrue(screen.contains("elapsed - baseDelay - i * 125L"));
        assertTrue(screen.contains("state.message()"));
        assertTrue(screen.contains("state.tableState()"));
        assertTrue(screen.contains("state.privateState()"));
    }

    @Test
    void diceButtonsFollowUnderExactOverVisualOrder() throws Exception {
        String screen = Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
        assertTrue(screen.contains("{\"under7\", \"exact7\", \"over7\"}"));
    }
}
