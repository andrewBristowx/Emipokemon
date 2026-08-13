package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha51SingleLayerRegressionTest {
    private static String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void rouletteDrawsOneStaticCompositionAndInvisibleHitZones() throws Exception {
        String source = screen();
        assertTrue(source.contains("drawAsset(context, ROULETTE_ALPHA51_BACKGROUND"));
        assertFalse(source.contains("drawAsset(context, ROULETTE_LEFT_PANEL"));
        assertFalse(source.contains("drawAsset(context, ROULETTE_SIDE_PANEL"));
        assertTrue(source.contains("new QuickChipZone(panelX + refX(425)"));
        assertTrue(source.contains("new RouletteCell(zeroX"));
    }

    @Test
    void staticWheelAndBallUseTheSharedServerResult() throws Exception {
        String source = screen();
        assertTrue(source.contains("rouletteResultNumber()"));
        assertTrue(source.contains("state.recentResults()"));
        assertTrue(source.contains("RotationAxis.POSITIVE_Z.rotation"));
        assertTrue(source.contains("double resultAngle = TAU * resultIndex / ROULETTE_WHEEL.length"));
        assertTrue(source.contains("drawRouletteBall(context, size)"));
    }

    @Test
    void dynamicLabelsUseTheBundledCasinoFont() throws Exception {
        String source = screen();
        assertTrue(source.contains("Style.EMPTY.withFont(CASINO_FONT)"));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/emipokemon/font/casino.json")));
        assertTrue(Files.isRegularFile(Path.of("LICENSES/OFL-PixelifySans.txt")));
    }
}
