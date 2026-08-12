package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha52AlignmentRegressionTest {
    private static String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void wheelLabelsStayCenteredAndBallUsesBrownTrack() throws Exception {
        String source = screen();
        assertTrue(source.contains("size * 0.286F"));
        assertTrue(source.contains("wheelSize * 0.370D"));
        assertFalse(source.contains("drawAsset(context, ROULETTE_MEDALLION"));
        assertTrue(source.contains("rouletteResultNumber()"));
    }

    @Test
    void variableRoundTextIsFittedInsideItsReservedWells() throws Exception {
        String source = screen();
        assertTrue(source.contains("drawFittedCenteredUiText(context, phaseLabel()"));
        assertTrue(source.contains("badgeW - refX(12)"));
        assertTrue(source.contains("drawFittedCenteredUiText(context, timer"));
    }

    @Test
    void casinoFontUsesTheCompactApprovedSize() throws Exception {
        String font = Files.readString(Path.of("src/main/resources/assets/emipokemon/font/casino.json"));
        assertTrue(font.contains("\"size\": 9.0"));
    }
}
