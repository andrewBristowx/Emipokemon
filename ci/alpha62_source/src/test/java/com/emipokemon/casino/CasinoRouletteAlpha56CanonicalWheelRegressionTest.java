package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha56CanonicalWheelRegressionTest {
    private static final int[] ORDER = {0,32,15,19,4,21,2,25,17,34,6,27,13,36,11,30,8,23,10,5,24,16,33,1,20,14,31,9,22,18,29,7,28,12,35,3,26};
    private static final Set<Integer> RED = Set.of(1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36);

    @Test
    void everyRenderedPocketHasItsOfficialEuropeanColor() throws Exception {
        BufferedImage image = ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/gui/casino/roulette_alpha51_wheel.png").toFile());
        double center = image.getWidth() / 2.0;
        double radius = 390.0;
        for (int i = 0; i < ORDER.length; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / ORDER.length;
            int rgb = image.getRGB((int)Math.round(center + Math.cos(angle) * radius),
                    (int)Math.round(center + Math.sin(angle) * radius));
            int red = (rgb >>> 16) & 255;
            int green = (rgb >>> 8) & 255;
            String actual = green > red ? "green" : red > 100 ? "red" : "black";
            String expected = ORDER[i] == 0 ? "green" : RED.contains(ORDER[i]) ? "red" : "black";
            assertEquals(expected, actual, "wrong pocket color for " + ORDER[i]);
        }
        assertEquals(26, ORDER[36]);
        assertTrue(!RED.contains(26));
    }
}
