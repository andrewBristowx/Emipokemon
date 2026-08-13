package com.emipokemon.casino;

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
        assertFalse(screen.contains("        renderBackground(context, mouseX, mouseY, delta);"));
        assertTrue(screen.contains("context.fill(0, 0, width, height, BACKDROP)"));
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
            bodyColors.add(image.getRGB(100, 60) & 0xFFFFFF);
            assertTrue(luma(body) >= 62.0, model + " main body is still too dark: " + Integer.toHexString(body));
            assertNotEquals(0xFF00FF, accent, model + " must not use missing-texture magenta as its accent");
            assertTrue(luma(accent) >= 85.0, model + " accent is too dark");
        }
        assertEquals(models.size(), bodyColors.size(), "every casino machine needs a distinct game-detail identity");
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
