from pathlib import Path
import shutil


root = Path(".")
ci = Path(__file__).resolve().parent

for relative in ("gradle.properties", "src/main/java/com/emipokemon/Emipokemon.java"):
    path = root / relative
    text = path.read_text(encoding="utf-8")
    if "0.4.0-alpha.58" not in text:
        raise AssertionError(f"missing alpha.58 marker in {relative}")
    path.write_text(text.replace("0.4.0-alpha.58", "0.4.0-alpha.59"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.58", "0.4.0-alpha.59")
    text = text.replace("alpha58VersionIsConsistent", "alpha59VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

shutil.copyfile(
    ci / "alpha59" / "CasinoScreen.java",
    root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java",
)

release_dir = root / "release/0.4.0-alpha.59"
release_dir.mkdir(parents=True, exist_ok=True)
for name in ("CAMBIOS-0.4.0-alpha.59.md", "GUIA-INSTALACION-Y-PRUEBAS.md", "GUIA-PRUEBA-ANIMACIONES.md"):
    shutil.copyfile(ci / "alpha59" / name, release_dir / name)

regression = root / "src/test/java/com/emipokemon/casino/CasinoAlpha59AnimationRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoAlpha59AnimationRegressionTest {
    @Test
    void animationsResolveToServerProvidedResults() throws Exception {
        String screen = Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
        assertTrue(screen.contains("drawSlotSymbol"));
        assertTrue(screen.contains("stopAt = 850L + i * 300L"));
        assertTrue(screen.contains("spinning ? cycle"));
        assertTrue(screen.contains("drawAnimatedDie"));
        assertTrue(screen.contains("rolling ? 1 +"));
        assertTrue(screen.contains("diceValues()"));
        assertTrue(screen.contains("drawCardsInSlots"));
        assertTrue(screen.contains("drawCardFace"));
        assertTrue(screen.contains("elapsed - baseDelay - i * 125L"));
        assertTrue(screen.contains("state.message()"));
        assertTrue(screen.contains("state.tableState()"));
        assertTrue(screen.contains("state.privateState()"));
    }

    @Test
    void diceButtonsFollowUnderExactOverVisualOrder() throws Exception {
        String screen = Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
        assertTrue(screen.contains("{\"under7\", \"exact7\", \"over7\"}"));
    }
}
''', encoding="utf-8")

print("alpha.59 casino animations and crisp game symbols installed")
