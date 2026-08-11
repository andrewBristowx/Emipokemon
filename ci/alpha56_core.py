from pathlib import Path
from math import atan2, floor, hypot, pi
import shutil

from PIL import Image


root = Path(".")
ci = Path(__file__).resolve().parent


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise AssertionError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


for relative in ("gradle.properties", "src/main/java/com/emipokemon/Emipokemon.java"):
    path = root / relative
    text = path.read_text(encoding="utf-8")
    if "0.4.0-alpha.55" not in text:
        raise AssertionError(f"missing alpha.55 marker in {relative}")
    path.write_text(text.replace("0.4.0-alpha.55", "0.4.0-alpha.56"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.55", "0.4.0-alpha.56")
    text = text.replace("alpha55VersionIsConsistent", "alpha56VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

screen_path = root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java"
s = screen_path.read_text(encoding="utf-8")
s = replace_once(s, "import net.minecraft.text.Style;\n", "import net.minecraft.text.OrderedText;\nimport net.minecraft.text.Style;\n", "ordered text import")
s = replace_once(s, "    private static final int MUTED = 0xFFCABEA9;", "    private static final int MUTED = 0xFFE2D8C7;", "brighter secondary text")
s = replace_once(s, "        double radius = wheelSize * 0.398D;", "        double radius = wheelSize * 0.370D;", "brown track radius")

old_helpers = '''    private void drawUiText(DrawContext context, String value, int x, int y, int color, boolean bold) {
        Text text = casinoText(value, bold);
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    private void drawCenteredUiText(DrawContext context, String value, int centerX, int y, int color, boolean bold) {
        Text text = casinoText(value, bold);
        context.drawTextWithShadow(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color);
    }

    private void drawFittedCenteredUiText(DrawContext context, String value, int centerX, int y,
                                          int maxWidth, int color, boolean bold) {
        Text text = casinoText(value, bold);
        int textWidth = Math.max(1, textRenderer.getWidth(text));
        float scale = Math.min(1.0F, maxWidth / (float)textWidth);
        context.getMatrices().push();
        context.getMatrices().translate(centerX, y, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawTextWithShadow(textRenderer, text, -textWidth / 2, 0, color);
        context.getMatrices().pop();
    }'''
new_helpers = '''    private void drawReadableText(DrawContext context, Text text, int x, int y, int color) {
        int outline = 0xE0100716;
        context.drawText(textRenderer, text, x - 1, y, outline, false);
        context.drawText(textRenderer, text, x + 1, y, outline, false);
        context.drawText(textRenderer, text, x, y - 1, outline, false);
        context.drawText(textRenderer, text, x, y + 1, outline, false);
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    private void drawReadableText(DrawContext context, OrderedText text, int x, int y, int color) {
        int outline = 0xE0100716;
        context.drawText(textRenderer, text, x - 1, y, outline, false);
        context.drawText(textRenderer, text, x + 1, y, outline, false);
        context.drawText(textRenderer, text, x, y - 1, outline, false);
        context.drawText(textRenderer, text, x, y + 1, outline, false);
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    private void drawUiText(DrawContext context, String value, int x, int y, int color, boolean bold) {
        Text text = casinoText(value, bold);
        drawReadableText(context, text, x, y, color);
    }

    private void drawCenteredUiText(DrawContext context, String value, int centerX, int y, int color, boolean bold) {
        Text text = casinoText(value, bold);
        drawReadableText(context, text, centerX - textRenderer.getWidth(text) / 2, y, color);
    }

    private void drawFittedCenteredUiText(DrawContext context, String value, int centerX, int y,
                                          int maxWidth, int color, boolean bold) {
        Text text = casinoText(value, bold);
        int textWidth = Math.max(1, textRenderer.getWidth(text));
        float scale = Math.min(1.0F, maxWidth / (float)textWidth);
        context.getMatrices().push();
        context.getMatrices().translate(centerX, y, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        drawReadableText(context, text, -textWidth / 2, 0, color);
        context.getMatrices().pop();
    }'''
s = replace_once(s, old_helpers, new_helpers, "readable casino text helpers")
s = s.replace("            context.drawTextWithShadow(textRenderer, line, x, y, color);", "            drawReadableText(context, line, x, y, color);")
s = s.replace("            context.drawTextWithShadow(textRenderer, line, 0, localY, color);", "            drawReadableText(context, line, 0, localY, color);")
s = replace_once(
    s,
    "            context.drawTextWithShadow(textRenderer, numberText, -textRenderer.getWidth(numberText) / 2, -4, WHITE);",
    "            drawReadableText(context, numberText, -textRenderer.getWidth(numberText) / 2, -4, WHITE);",
    "readable wheel labels",
)
s = s.replace(
    '            context.drawTextWithShadow(textRenderer, casinoText("Esperando apuestas…", false), sideX + sidePx(34), y, MUTED);',
    '            drawReadableText(context, casinoText("Esperando apuestas…", false), sideX + sidePx(34), y, MUTED);',
)
s = s.replace(
    "            context.drawTextWithShadow(textRenderer, casinoText(clipped, false), sideX + sidePx(41), y + 1, WHITE);",
    "            drawReadableText(context, casinoText(clipped, false), sideX + sidePx(41), y + 1, WHITE);",
)
s = s.replace(
    '            context.drawTextWithShadow(textRenderer, casinoText("+" + (players.size() - shown) + " más", false), sideX + sidePx(41), y, MUTED);',
    '            drawReadableText(context, casinoText("+" + (players.size() - shown) + " más", false), sideX + sidePx(41), y, MUTED);',
)
screen_path.write_text(s, encoding="utf-8")

font_path = root / "src/main/resources/assets/emipokemon/font/casino.json"
font = font_path.read_text(encoding="utf-8")
font = replace_once(font, '"oversample": 2.0', '"oversample": 4.0', "sharper casino font sampling")
font_path.write_text(font, encoding="utf-8")

# Rebuild only the numbered pocket band. The original artwork has irregular sector
# geometry and gives both neighbours of zero a red pocket. Canonical indices are
# now generated from the same European order used by the server and the renderer.
wheel_path = root / "src/main/resources/assets/emipokemon/textures/gui/casino/roulette_alpha51_wheel.png"
image = Image.open(wheel_path).convert("RGBA")
pixels = image.load()
cx = image.width / 2.0
cy = image.height / 2.0
inner_radius = 340.0
outer_radius = 444.0
step = pi * 2.0 / 37.0
red_numbers = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36}
order = [0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10,
         5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26]

for y in range(max(0, int(cy - outer_radius - 2)), min(image.height, int(cy + outer_radius + 3))):
    for x in range(max(0, int(cx - outer_radius - 2)), min(image.width, int(cx + outer_radius + 3))):
        dx = x - cx
        dy = y - cy
        radius = hypot(dx, dy)
        if radius < inner_radius or radius > outer_radius:
            continue
        position = ((atan2(dy, dx) + pi / 2.0) % (pi * 2.0)) / step
        index = int(floor(position + 0.5)) % 37
        fraction = position - floor(position)
        edge_distance = abs(fraction - 0.5) * step * radius
        if edge_distance < 2.2 or radius - inner_radius < 2.5 or outer_radius - radius < 2.5:
            color = (246, 183, 36)
        else:
            number = order[index]
            if number == 0:
                color = (4, 132, 52)
            elif number in red_numbers:
                color = (215, 18, 30)
            else:
                color = (17, 16, 20)
            radial_light = 0.92 + 0.10 * ((radius - inner_radius) / (outer_radius - inner_radius))
            color = tuple(max(0, min(255, round(channel * radial_light))) for channel in color)
        pixels[x, y] = (*color, pixels[x, y][3])

image.save(wheel_path, optimize=True)

for relative in (
    "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha52AlignmentRegressionTest.java",
    "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha53PanelBoundsRegressionTest.java",
    "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha55BallOnlyRegressionTest.java",
):
    path = root / relative
    if path.exists():
        text = path.read_text(encoding="utf-8").replace("wheelSize * 0.398D", "wheelSize * 0.370D")
        path.write_text(text, encoding="utf-8")

release_dir = root / "release/0.4.0-alpha.56"
release_dir.mkdir(parents=True, exist_ok=True)
for name in ("CAMBIOS-0.4.0-alpha.56.md", "GUIA-INSTALACION-Y-PRUEBAS.md", "GUIA-PRUEBA-VISUAL.md"):
    shutil.copyfile(ci / "alpha56" / name, release_dir / name)

regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha56CanonicalWheelRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

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
''', encoding="utf-8")

print("alpha.56 canonical wheel colors, inner brown track and readable text installed")
