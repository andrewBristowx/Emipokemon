package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoAlpha57PersonalizedGamesRegressionTest {
    @Test
    void everyImplementedCasinoGameHasItsOwnVisualShowcase() throws Exception {
        String screen = Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
        for (String marker : new String[] {
                "drawSlotShowcase", "drawChipExchangeShowcase", "drawTicketShowcase",
                "drawDiceShowcase", "drawCardShowcase", "CasinoTheme"
        }) assertTrue(screen.contains(marker), marker);
        for (String game : new String[] {
                "slot", "chip_exchange", "ticket_exchange", "dice", "blackjack", "poker"
        }) assertTrue(screen.contains("\"" + game + "\""), game);
        assertTrue(screen.contains("state.message()"));
        assertTrue(screen.contains("state.tableState()"));
        assertTrue(screen.contains("state.privateState()"));
        assertTrue(screen.contains("send(cell.action)"));
    }
}
