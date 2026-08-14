from pathlib import Path
import shutil


root = Path(".")
ci = Path(__file__).resolve().parent

for relative in ("gradle.properties", "src/main/java/com/emipokemon/Emipokemon.java"):
    path = root / relative
    text = path.read_text(encoding="utf-8")
    if "0.4.0-alpha.59" not in text:
        raise AssertionError(f"missing alpha.59 marker in {relative}")
    path.write_text(text.replace("0.4.0-alpha.59", "0.4.0-alpha.60"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.59", "0.4.0-alpha.60")
    text = text.replace("alpha59VersionIsConsistent", "alpha60VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

shutil.copyfile(
    ci / "alpha60" / "CasinoScreen.java",
    root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java",
)

asset_target = root / "src/main/resources/assets/emipokemon/textures/gui/casino/finished/slot_symbols.png"
asset_target.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(ci / "alpha60" / "assets" / "slot_symbols.png", asset_target)

release_dir = root / "release/0.4.0-alpha.60"
release_dir.mkdir(parents=True, exist_ok=True)
for name in ("CAMBIOS-0.4.0-alpha.60.md", "GUIA-INSTALACION-Y-PRUEBAS.md", "GUIA-PRUEBA-CASINO.md"):
    shutil.copyfile(ci / "alpha60" / name, release_dir / name)

regression = root / "src/test/java/com/emipokemon/casino/CasinoAlpha60VisualRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

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
        assertTrue(screen.contains("hasDiceResult() && elapsed < 2200L"));
        assertTrue(screen.contains("float bounce"));
        assertTrue(screen.contains("float travel"));
        assertTrue(screen.contains("float angle"));
        assertTrue(screen.contains("diceValues()"));
        assertTrue(screen.contains("{\"under7\", \"exact7\", \"over7\"}"));
        assertTrue(screen.contains("drawFinishedControlLabel"));
    }
}
''', encoding="utf-8")

print("alpha.60 casino symbols, text layout and dice animation installed")
