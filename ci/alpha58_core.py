from pathlib import Path
import shutil


root = Path(".")
ci = Path(__file__).resolve().parent

for relative in ("gradle.properties", "src/main/java/com/emipokemon/Emipokemon.java"):
    path = root / relative
    text = path.read_text(encoding="utf-8")
    if "0.4.0-alpha.57" not in text:
        raise AssertionError(f"missing alpha.57 marker in {relative}")
    path.write_text(text.replace("0.4.0-alpha.57", "0.4.0-alpha.58"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.57", "0.4.0-alpha.58")
    text = text.replace("alpha57VersionIsConsistent", "alpha58VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

shutil.copyfile(
    ci / "alpha58" / "CasinoScreen.java",
    root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java",
)

asset_target = root / "src/main/resources/assets/emipokemon/textures/gui/casino/finished"
asset_target.mkdir(parents=True, exist_ok=True)
for asset in (ci / "alpha58" / "assets").glob("*.png"):
    shutil.copyfile(asset, asset_target / asset.name)

release_dir = root / "release/0.4.0-alpha.58"
release_dir.mkdir(parents=True, exist_ok=True)
for name in ("CAMBIOS-0.4.0-alpha.58.md", "GUIA-INSTALACION-Y-PRUEBAS.md", "GUIA-PRUEBA-VISUAL.md"):
    shutil.copyfile(ci / "alpha58" / name, release_dir / name)

regression = root / "src/test/java/com/emipokemon/casino/CasinoAlpha58FinishedAssetsRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

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
''', encoding="utf-8")

print("alpha.58 finished casino assets and dynamic interaction layers installed")
