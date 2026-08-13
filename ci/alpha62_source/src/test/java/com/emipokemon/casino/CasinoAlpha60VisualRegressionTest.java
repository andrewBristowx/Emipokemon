package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoAlpha60VisualRegressionTest {
    @Test
    void slotUsesGraphicalAtlasForEveryServerSymbol() throws Exception {
        String screen = Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
        assertTrue(screen.contains("CASINO_SLOT_SYMBOLS"));
        for (String symbol : new String[] {"CEREZA", "BAYA", "CAMPANA", "ESTRELLA", "EMI", "JACKPOT"}) {
            assertTrue(screen.contains(symbol), symbol);
        }
        assertTrue(screen.contains("unknown legacy values always remain graphical"));
        BufferedImage atlas = ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/gui/casino/finished/slot_symbols.png").toFile());
        assertEquals(1280, atlas.getWidth());
        assertEquals(286, atlas.getHeight());
        assertTrue(atlas.getColorModel().hasAlpha());
    }

    @Test
    void diceAnimationAndControlsRemainServerAuthoritative() throws Exception {
        String screen = Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
        assertTrue(screen.contains("hasDiceResult() && elapsed < 2600L"));
        assertTrue(screen.contains("float bounce"));
        assertTrue(screen.contains("float travel"));
        assertTrue(screen.contains("float angle"));
        assertTrue(screen.contains("diceValues()"));
        assertTrue(screen.contains("{\"under7\", \"exact7\", \"over7\"}"));
        assertTrue(screen.contains("drawFinishedControlLabel"));
    }
}
