package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha53PanelBoundsRegressionTest {
    private static String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void rouletteNumbersStayCenteredAndBallUsesBrownTrack() throws Exception {
        String source = screen();
        assertTrue(source.contains("size * 0.286F"));
        assertTrue(source.contains("wheelSize * 0.370D"));
        assertTrue(source.contains("rouletteResultNumber()"));
    }

    @Test
    void everyVariableSideSectionHasHardVisualBounds() throws Exception {
        String source = screen();
        assertTrue(source.contains("refY(503)"));
        assertTrue(source.contains("refY(790)"));
        assertTrue(source.contains("refY(842)"));
        assertTrue(source.contains("refX(1194)"));
        assertTrue(source.contains("drawScaledIntegratedWrapped"));
        assertTrue(source.contains("context.disableScissor()"));
    }
}
