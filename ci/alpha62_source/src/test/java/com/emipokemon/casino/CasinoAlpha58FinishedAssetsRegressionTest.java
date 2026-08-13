package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoAlpha58FinishedAssetsRegressionTest {
    @Test
    void everyRealGameHasAFullFinishedOpaqueComposition() throws Exception {
        Path assets = Path.of("src/main/resources/assets/emipokemon/textures/gui/casino/finished");
        for (String name : new String[] {"poker", "blackjack", "dice", "slot", "chip_exchange", "ticket_exchange"}) {
            Path path = assets.resolve("casino_" + name + ".png");
            assertTrue(Files.isRegularFile(path), name);
            BufferedImage image = ImageIO.read(path.toFile());
            assertEquals(1536, image.getWidth(), name);
            assertEquals(1024, image.getHeight(), name);
            assertEquals(255, (image.getRGB(0, 0) >>> 24) & 255, name + " alpha");
        }
    }

    @Test
    void finishedArtKeepsServerStateAndClickableControlsDynamic() throws Exception {
        String screen = Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
        assertTrue(screen.contains("renderFinishedGame"));
        assertTrue(screen.contains("finishedActionControls"));
        assertTrue(screen.contains("state.tableState()"));
        assertTrue(screen.contains("state.privateState()"));
        assertTrue(screen.contains("send(control.action())"));
        assertTrue(screen.contains("setQuickAmount"));
    }
}
