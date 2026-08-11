from math import hypot
from pathlib import Path
import shutil

from PIL import Image


root = Path(".")
ci = Path(__file__).resolve().parent

for relative in ("gradle.properties", "src/main/java/com/emipokemon/Emipokemon.java"):
    path = root / relative
    text = path.read_text(encoding="utf-8")
    if "0.4.0-alpha.56" not in text:
        raise AssertionError(f"missing alpha.56 marker in {relative}")
    path.write_text(text.replace("0.4.0-alpha.56", "0.4.0-alpha.57"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.56", "0.4.0-alpha.57")
    text = text.replace("alpha56VersionIsConsistent", "alpha57VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

screen_target = root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java"
shutil.copyfile(ci / "alpha57" / "CasinoScreen.java", screen_target)

# Alpha.56 rebuilt the official pocket colors, but also painted over two original
# gold boundary arcs. Restore only those narrow annuli from the untouched artwork.
wheel_path = root / "src/main/resources/assets/emipokemon/textures/gui/casino/roulette_alpha51_wheel.png"
original_path = ci / "assets" / "roulette_alpha51_wheel.png"
wheel = Image.open(wheel_path).convert("RGBA")
original = Image.open(original_path).convert("RGBA")
if wheel.size != original.size:
    raise AssertionError("roulette wheel asset dimensions changed")
pixels = wheel.load()
source = original.load()
cx = wheel.width / 2.0
cy = wheel.height / 2.0
for y in range(wheel.height):
    for x in range(wheel.width):
        radius = hypot(x - cx, y - cy)
        if 338.0 <= radius <= 350.0 or 436.0 <= radius <= 450.0:
            pixels[x, y] = source[x, y]
wheel.save(wheel_path, optimize=True)

release_dir = root / "release/0.4.0-alpha.57"
release_dir.mkdir(parents=True, exist_ok=True)
for name in ("CAMBIOS-0.4.0-alpha.57.md", "GUIA-INSTALACION-Y-PRUEBAS.md", "GUIA-PRUEBA-VISUAL.md"):
    shutil.copyfile(ci / "alpha57" / name, release_dir / name)

regression = root / "src/test/java/com/emipokemon/casino/CasinoAlpha57PersonalizedGamesRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

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
''', encoding="utf-8")

print("alpha.57 personalized casino screens and restored roulette gold arcs installed")
