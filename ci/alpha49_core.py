from pathlib import Path

root = Path(".")

gradle = root / "gradle.properties"
text = gradle.read_text(encoding="utf-8")
assert "mod_version=0.4.0-alpha.48" in text
gradle.write_text(text.replace("mod_version=0.4.0-alpha.48", "mod_version=0.4.0-alpha.49"), encoding="utf-8")

core = root / "src/main/java/com/emipokemon/Emipokemon.java"
text = core.read_text(encoding="utf-8")
assert "0.4.0-alpha.48" in text
core.write_text(text.replace("0.4.0-alpha.48", "0.4.0-alpha.49"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.48", "0.4.0-alpha.49")
    text = text.replace("alpha48VersionIsConsistent", "alpha49VersionIsConsistent")
    text = text.replace(
        'assertTrue(s.contains("drawCenteredUiText(context, \\"ÚLTIMOS\\""));',
        'assertTrue(s.contains("Últimos resultados"));'
    )
    text = text.replace(
        'rouletteContentH = Math.max(334, panelH - rouletteHeaderH - 6)',
        'rouletteContentH = panelY + panelH - contentTop'
    )
    test.write_text(text, encoding="utf-8")

regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha49LayoutRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CasinoRouletteAlpha49LayoutRegressionTest {
    private String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void splitHdArtFillsWidthAndPreservesOriginalBottomEdge() throws Exception {
        String s = screen();
        assertTrue(s.contains("gameX = panelX"));
        assertTrue(s.contains("sideX = gameX + gameW"));
        assertTrue(s.contains("sideW = panelX + panelW - sideX"));
        assertTrue(s.contains("contentTop -= rouletteOverlapH"));
        assertFalse(s.contains("renderRouletteEdgeCleanup(context)"));
        assertFalse(s.contains("private void renderRouletteEdgeCleanup"));
    }

    @Test
    void fieldValueIsRenderedOnceAndInformationStaysInDedicatedSlots() throws Exception {
        String s = screen();
        assertTrue(s.contains("amountField.visible = false"));
        assertTrue(s.contains("renderWagerAmount(context)"));
        assertTrue(s.contains("drawFittedCenteredUiText(context, value"));
        assertTrue(s.contains("Últimos resultados"));
        assertTrue(s.contains("renderPlayerCount(context, players.size())"));
        assertTrue(s.contains("Inicia en:"));
    }

    @Test
    void rouletteCapacityMatchesTheLiveServerBackedCounter() throws Exception {
        String server = Files.readString(Path.of("src/main/java/com/emipokemon/casino/CasinoTableService.java"));
        assertTrue(server.contains("MAX_ROULETTE_PLAYERS = 8"));
        assertTrue(server.contains("session.participants.size() >= MAX_ROULETTE_PLAYERS"));
        assertTrue(server.indexOf("session.participants.size() >= MAX_ROULETTE_PLAYERS")
                < server.indexOf("if (!reserve(player, amount"));
    }
}
''', encoding="utf-8")
