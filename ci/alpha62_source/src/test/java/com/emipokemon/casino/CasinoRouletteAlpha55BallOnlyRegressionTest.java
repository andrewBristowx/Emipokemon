package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha55BallOnlyRegressionTest {
    private static String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void wheelAndCenterStayStaticWhileBallTargetsServerResult() throws Exception {
        String source = screen();
        assertFalse(source.contains("rouletteWheelRotation()"));
        assertFalse(source.contains("rotation((float)rotation)"));
        assertTrue(source.contains("double radius = wheelSize * 0.370D"));
        assertTrue(source.contains("ROULETTE_WHEEL[i] == result"));
        assertTrue(source.contains("double resultAngle = TAU * resultIndex / ROULETTE_WHEEL.length"));
    }

    @Test
    void balanceAndTicketTextStayInsideTheirImageWells() throws Exception {
        String source = screen();
        assertTrue(source.contains("refX(1262)"));
        assertTrue(source.contains("refX(145)"));
        assertTrue(source.contains("refX(1182)"));
        assertTrue(source.contains("refY(520)"));
        assertTrue(source.contains("refY(544)"));
    }
}
