from pathlib import Path

root = Path('.')

def read(rel):
    return (root / rel).read_text()

def write(rel, text):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text)

# Advance inherited version assertions from alpha.36 to alpha.37.
for rel in [
    'src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java',
    'src/test/java/com/emipokemon/casino/CasinoMultiplayerRegressionTest.java',
]:
    p = root / rel
    if p.exists():
        s = p.read_text()
        s = s.replace('alpha36VersionIsConsistentInSource', 'alpha37VersionIsConsistentInSource')
        s = s.replace('alpha36VersionIsConsistent', 'alpha37VersionIsConsistent')
        s = s.replace('0.4.0-alpha.36', '0.4.0-alpha.37')
        p.write_text(s)

write('src/test/java/com/emipokemon/casino/CasinoVisualRegressionTest.java', r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CasinoVisualRegressionTest {
    private String source(String relative) throws Exception {
        return Files.readString(Path.of("src", relative));
    }

    @Test
    void alpha37CasinoScreenDoesNotBlurItsOwnUi() throws Exception {
        String screen = source("client/java/com/emipokemon/client/casino/CasinoScreen.java");
        assertTrue(screen.contains("public void renderBackground(DrawContext context"));
        assertTrue(screen.contains("context.fill(0, 0, width, height, 0x99000000)"));
        assertFalse(screen.contains("        renderBackground(context, mouseX, mouseY, delta);"));
        assertTrue(screen.contains("private static final int PANEL = 0xFF160B1E"));
        assertTrue(screen.contains("private static final int WHITE = 0xFFFFFFFF"));
    }

    @Test
    void alpha37CasinoAtlasesAreBrightDistinctAndValid() throws Exception {
        List<String> models = List.of("slot", "chip_exchange", "ticket_exchange", "roulette", "poker", "blackjack", "dice");
        Set<Integer> bodyColors = new HashSet<>();
        for (String model : models) {
            Path path = Path.of("src/main/resources/assets/emipokemon/textures/block/casino_" + model + ".png");
            BufferedImage image = ImageIO.read(path.toFile());
            assertNotNull(image, model + " texture must decode");
            assertEquals(128, image.getWidth(), model);
            assertEquals(128, image.getHeight(), model);

            int body = image.getRGB(10, 10) & 0xFFFFFF;
            int accent = image.getRGB(60, 20) & 0xFFFFFF;
            bodyColors.add(body);
            assertTrue(luma(body) >= 62.0, model + " main body is still too dark: " + Integer.toHexString(body));
            assertNotEquals(0xFF00FF, accent, model + " must not use missing-texture magenta as its accent");
            assertTrue(luma(accent) >= 85.0, model + " accent is too dark");
        }
        assertEquals(models.size(), bodyColors.size(), "every casino machine needs a distinct body identity");
    }

    @Test
    void alpha37KeepsSharedCasinoBackendFromAlpha36() throws Exception {
        String table = source("main/java/com/emipokemon/casino/CasinoTableService.java");
        assertTrue(table.contains("RANDOM.nextInt(37)"));
        assertTrue(table.contains("MAX_BLACKJACK_PLAYERS = 5"));
        assertTrue(table.contains("MAX_POKER_PLAYERS = 6"));
        assertTrue(table.contains("refund(participant.id"));
    }

    private static double luma(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }
}
''')
