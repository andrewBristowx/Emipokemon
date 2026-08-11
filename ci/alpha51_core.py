from pathlib import Path
import shutil


root = Path(".")
ci = Path(__file__).resolve().parent


def replace_version(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    if "0.4.0-alpha.50" not in text:
        raise AssertionError(f"missing alpha.50 marker in {path}")
    path.write_text(text.replace("0.4.0-alpha.50", "0.4.0-alpha.51"), encoding="utf-8")


replace_version(root / "gradle.properties")
replace_version(root / "src/main/java/com/emipokemon/Emipokemon.java")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.50", "0.4.0-alpha.51")
    text = text.replace("alpha50VersionIsConsistent", "alpha51VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

# alpha.51 replaces only the roulette client renderer. All server-authoritative casino
# state, bets, balances, duplicate protection and persistence remain on alpha.50.
screen_source = ci / "alpha51" / "CasinoScreen.java"
screen_target = root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java"
shutil.copyfile(screen_source, screen_target)

texture_dir = root / "src/main/resources/assets/emipokemon/textures/gui/casino"
texture_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(ci / "assets/roulette_alpha51_background.png", texture_dir / "roulette_alpha51_background.png")
shutil.copyfile(ci / "assets/roulette_alpha51_wheel.png", texture_dir / "roulette_alpha51_wheel.png")

font_dir = root / "src/main/resources/assets/emipokemon/font"
font_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(ci / "assets/font/PixelifySans-wght.ttf", font_dir / "pixelify_sans.ttf")
shutil.copyfile(ci / "assets/font/OFL-PixelifySans.txt", font_dir / "OFL-PixelifySans.txt")
(font_dir / "casino.json").write_text(
    '''{\n  "providers": [\n    {\n      "type": "ttf",\n      "file": "emipokemon:pixelify_sans.ttf",\n      "shift": [0.0, 0.0],\n      "size": 11.0,\n      "oversample": 2.0\n    }\n  ]\n}\n''',
    encoding="utf-8",
)

license_dir = root / "LICENSES"
license_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(ci / "assets/font/OFL-PixelifySans.txt", license_dir / "OFL-PixelifySans.txt")

release_dir = root / "release/0.4.0-alpha.51"
release_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(ci / "alpha51/CAMBIOS-0.4.0-alpha.51.md", release_dir / "CAMBIOS-0.4.0-alpha.51.md")
shutil.copyfile(ci / "alpha51/GUIA-INSTALACION-Y-PRUEBAS.md", release_dir / "GUIA-INSTALACION-Y-PRUEBAS.md")

# The alpha.50 viewport test describes the superseded renderer. Alpha.51 has its own
# exact single-layer, hitbox and authoritative-animation regression contract.
old_test = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha50ViewportRegressionTest.java"
if old_test.exists():
    old_test.unlink()

integrated = root / "src/test/java/com/emipokemon/casino/CasinoRouletteIntegratedUiRegressionTest.java"
if integrated.exists():
    text = integrated.read_text(encoding="utf-8")
    text = text.replace('assertTrue(s.contains("drawAsset(context, ROULETTE_REFERENCE"));',
                        'assertTrue(s.contains("drawAsset(context, ROULETTE_ALPHA51_BACKGROUND"));')
    text = text.replace('assertTrue(s.contains("Últimos resultados"));',
                        'assertTrue(s.contains("drawRecentResults(context)"));')
    integrated.write_text(text, encoding="utf-8")

presentation = root / "src/test/java/com/emipokemon/casino/CasinoRoulettePresentationRegressionTest.java"
if presentation.exists():
    text = presentation.read_text(encoding="utf-8")
    text = text.replace('assertTrue(s.contains("drawAsset(context, ROULETTE_WHEEL_OUTER"));',
                        'assertTrue(s.contains("drawAsset(context, ROULETTE_ALPHA51_WHEEL"));')
    text = text.replace('assertTrue(s.contains("leftPx("));',
                        'assertTrue(s.contains("refX("));')
    presentation.write_text(text, encoding="utf-8")

regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha51SingleLayerRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha51SingleLayerRegressionTest {
    private static String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void rouletteDrawsOneStaticCompositionAndInvisibleHitZones() throws Exception {
        String source = screen();
        assertTrue(source.contains("drawAsset(context, ROULETTE_ALPHA51_BACKGROUND"));
        assertFalse(source.contains("drawAsset(context, ROULETTE_LEFT_PANEL"));
        assertFalse(source.contains("drawAsset(context, ROULETTE_SIDE_PANEL"));
        assertTrue(source.contains("new QuickChipZone(panelX + refX(425)"));
        assertTrue(source.contains("new RouletteCell(zeroX"));
    }

    @Test
    void wheelAndBallSettleOnTheSharedServerResult() throws Exception {
        String source = screen();
        assertTrue(source.contains("rouletteResultNumber()"));
        assertTrue(source.contains("state.recentResults()"));
        assertTrue(source.contains("RotationAxis.POSITIVE_Z.rotation"));
        assertTrue(source.contains("TAU * 7.0D + target"));
        assertTrue(source.contains("drawRouletteBall(context, size)"));
    }

    @Test
    void dynamicLabelsUseTheBundledCasinoFont() throws Exception {
        String source = screen();
        assertTrue(source.contains("Style.EMPTY.withFont(CASINO_FONT)"));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/emipokemon/font/casino.json")));
        assertTrue(Files.isRegularFile(Path.of("LICENSES/OFL-PixelifySans.txt")));
    }
}
''', encoding="utf-8")

print("alpha.51 single-layer roulette renderer and licensed font installed")
