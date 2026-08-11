from pathlib import Path
root = Path('.')
p = root / 'src/test/java/com/emipokemon/casino/CasinoRouletteIntegratedUiRegressionTest.java'
p.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CasinoRouletteIntegratedUiRegressionTest {
    private String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void dynamicCasinoDataIsIntegratedIntoDedicatedSlots() throws Exception {
        String s = screen();
        assertTrue(s.contains("renderHeaderBalance(context)"));
        assertTrue(s.contains("renderWagerAmount(context)"));
        assertTrue(s.contains("renderPlayerCount(context, players.size())"));
        assertTrue(s.contains("count + \"/\" + ROULETTE_DISPLAY_CAPACITY"));
        assertTrue(s.contains("drawCenteredUiText(context, \"ÚLTIMOS\""));
        assertTrue(s.contains("amountField.isFocused()"));
        assertTrue(s.contains("rouletteContentH = Math.max(334, panelH - rouletteHeaderH - 6)"));
    }

    @Test
    void rouletteStillUsesServerBackedStateAndAuthoritativeActions() throws Exception {
        String s = screen();
        assertTrue(s.contains("state.balance()"));
        assertTrue(s.contains("state.players()"));
        assertTrue(s.contains("state.recentResults()"));
        assertTrue(s.contains("send(cell.action)"));
        assertTrue(s.contains("new CasinoNetworking.CasinoActionPayload"));
    }

    @Test
    void noBlurContractRemainsPresent() throws Exception {
        String s = screen();
        assertTrue(s.contains("public void renderBackground(DrawContext context"));
        assertTrue(s.contains("Deliberately empty: the casino draws its own dim backdrop"));
    }
}
''')
