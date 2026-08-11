from pathlib import Path
import shutil


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
    if "0.4.0-alpha.51" not in text:
        raise AssertionError(f"missing alpha.51 marker in {relative}")
    path.write_text(text.replace("0.4.0-alpha.51", "0.4.0-alpha.52"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.51", "0.4.0-alpha.52")
    text = text.replace("alpha51VersionIsConsistent", "alpha52VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

screen_path = root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java"
s = screen_path.read_text(encoding="utf-8")

s = replace_once(
    s,
    '''        drawCenteredUiText(context, phaseLabel(), x + badgeW / 2, y + refY(9),
                isBettingPhase() ? 0xFF9AF2B6 : 0xFFFFDB68, false);''',
    '''        drawFittedCenteredUiText(context, phaseLabel(), x + badgeW / 2, y + refY(9),
                badgeW - refX(12), isBettingPhase() ? 0xFF9AF2B6 : 0xFFFFDB68, false);''',
    "fitted round phase",
)
s = replace_once(
    s,
    '''            drawCenteredUiText(context, "Inicia en:", timerCenter, y, 0xFFF7F0E4, false);
            drawCenteredUiText(context, timer, timerCenter, y + refY(18), 0xFFFFE9A1, false);''',
    '''            drawFittedCenteredUiText(context, "Inicia en:", timerCenter, y, refX(92), 0xFFF7F0E4, false);
            drawFittedCenteredUiText(context, timer, timerCenter, y + refY(18), refX(92), 0xFFFFE9A1, false);''',
    "fitted round timer",
)
s = replace_once(
    s,
    '''            context.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 13;
        }
        return y;
    }

    private void drawRouletteWheel''',
    '''            context.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 11;
        }
        return y;
    }

    private void drawRouletteWheel''',
    "compact integrated line spacing",
)
s = replace_once(
    s,
    '''        float numberScale = Math.max(0.44F, Math.min(0.72F, size / 438.0F * 0.72F));
        int numberRadius = Math.round(size * 0.386F);''',
    '''        float numberScale = Math.max(0.62F, Math.min(0.76F, size / 438.0F * 0.82F));
        int numberRadius = Math.round(size * 0.334F);''',
    "wheel number placement",
)
s = replace_once(
    s,
    '''        int medSize = refX(116);''',
    '''        int medSize = refX(158);''',
    "stationary center coverage",
)
s = replace_once(
    s,
    '''        double radius = wheelSize * (0.456D - 0.055D * eased);''',
    '''        double radius = wheelSize * (0.456D - 0.118D * eased);''',
    "ball pocket radius",
)
screen_path.write_text(s, encoding="utf-8")

font_json = root / "src/main/resources/assets/emipokemon/font/casino.json"
font_text = font_json.read_text(encoding="utf-8")
font_text = replace_once(font_text, '"size": 11.0', '"size": 9.0', "casino font size")
font_json.write_text(font_text, encoding="utf-8")

release_dir = root / "release/0.4.0-alpha.52"
release_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(ci / "alpha52/CAMBIOS-0.4.0-alpha.52.md", release_dir / "CAMBIOS-0.4.0-alpha.52.md")
shutil.copyfile(ci / "alpha52/GUIA-PRUEBA-VISUAL.md", release_dir / "GUIA-PRUEBA-VISUAL.md")

regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha52AlignmentRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha52AlignmentRegressionTest {
    private static String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void wheelLabelsAndBallShareTheInnerPocketBand() throws Exception {
        String source = screen();
        assertTrue(source.contains("size * 0.334F"));
        assertTrue(source.contains("0.456D - 0.118D * eased"));
        assertTrue(source.contains("refX(158)"));
        assertTrue(source.contains("rouletteResultNumber()"));
    }

    @Test
    void variableRoundTextIsFittedInsideItsReservedWells() throws Exception {
        String source = screen();
        assertTrue(source.contains("drawFittedCenteredUiText(context, phaseLabel()"));
        assertTrue(source.contains("badgeW - refX(12)"));
        assertTrue(source.contains("drawFittedCenteredUiText(context, timer"));
    }

    @Test
    void casinoFontUsesTheCompactApprovedSize() throws Exception {
        String font = Files.readString(Path.of("src/main/resources/assets/emipokemon/font/casino.json"));
        assertTrue(font.contains("\"size\": 9.0"));
    }
}
''', encoding="utf-8")

print("alpha.52 roulette alignment and compact typography corrections installed")
