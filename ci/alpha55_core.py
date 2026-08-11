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
    if "0.4.0-alpha.54" not in text:
        raise AssertionError(f"missing alpha.54 marker in {relative}")
    path.write_text(text.replace("0.4.0-alpha.54", "0.4.0-alpha.55"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.54", "0.4.0-alpha.55")
    text = text.replace("alpha54VersionIsConsistent", "alpha55VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

screen_path = root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java"
s = screen_path.read_text(encoding="utf-8")

s = replace_once(
    s,
    '''        context.enableScissor(panelX + refX(1125), panelY + refY(526), panelX + refX(1490), panelY + refY(589));
        drawScaledIntegratedWrapped(context, own, panelX + refX(1194), panelY + refY(541),
                refX(280), WHITE, panelY + refY(561), 0.82F);
        if (selected.isBlank()) {
            drawScaledIntegratedWrapped(context, "Selecciona una casilla en el tapete",
                    panelX + refX(1194), panelY + refY(563), refX(280), MUTED, panelY + refY(586), 0.74F);
        }''',
    '''        context.enableScissor(panelX + refX(1125), panelY + refY(503), panelX + refX(1490), panelY + refY(589));
        drawScaledIntegratedWrapped(context, own, panelX + refX(1194), panelY + refY(520),
                refX(280), WHITE, panelY + refY(540), 0.82F);
        if (selected.isBlank()) {
            drawScaledIntegratedWrapped(context, "Selecciona una casilla en el tapete",
                    panelX + refX(1194), panelY + refY(544), refX(280), MUTED, panelY + refY(568), 0.74F);
        }''',
    "raise tu ficha text",
)

s = replace_once(
    s,
    '''        int centerX = panelX + refX(1245);
        int y = panelY + refY(76);
        String value = state.balance() + " Michicoins";
        drawFittedCenteredUiText(context, value, centerX, y, refX(172), 0xFFFFDF62, false);''',
    '''        int centerX = panelX + refX(1262);
        int y = panelY + refY(82);
        String value = state.balance() + " Michicoins";
        context.enableScissor(panelX + refX(1182), panelY + refY(62), panelX + refX(1340), panelY + refY(111));
        drawFittedCenteredUiText(context, value, centerX, y, refX(145), 0xFFFFDF62, false);
        context.disableScissor();''',
    "adaptive balance capsule",
)

s = replace_once(s, "        double rotation = rouletteWheelRotation();\n", "", "remove wheel rotation calculation")
s = replace_once(
    s,
    "        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float)rotation));\n",
    "",
    "keep wheel texture static",
)

s = replace_once(
    s,
    '''        double targetAngle = -Math.PI / 2.0D - TAU * 3.0D;
        double angle = freeAngle * (1.0D - eased) + targetAngle * eased;
        double radius = wheelSize * (0.398D - 0.108D * eased);''',
    '''        int resultIndex = 0;
        if (result >= 0) {
            for (int i = 0; i < ROULETTE_WHEEL.length; i++) {
                if (ROULETTE_WHEEL[i] == result) {
                    resultIndex = i;
                    break;
                }
            }
        }
        double resultAngle = TAU * resultIndex / ROULETTE_WHEEL.length - Math.PI / 2.0D;
        double targetAngle = resultAngle - TAU * 3.0D;
        double angle = freeAngle * (1.0D - eased) + targetAngle * eased;
        double radius = wheelSize * 0.398D;''',
    "ball-only result animation on brown track",
)

wheel_method = '''
    private double rouletteWheelRotation() {
        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAt);
        int result = rouletteResultNumber();
        if ("result".equals(state.phase()) && result >= 0) {
            int index = 0;
            for (int i = 0; i < ROULETTE_WHEEL.length; i++) if (ROULETTE_WHEEL[i] == result) { index = i; break; }
            double t = Math.min(1.0D, elapsed / 3200.0D);
            double eased = 1.0D - Math.pow(1.0D - t, 4.0D);
            double target = -TAU * index / ROULETTE_WHEEL.length;
            return (TAU * 7.0D + target) * eased;
        }
        return elapsed * 0.00135D;
    }
'''
s = replace_once(s, wheel_method, "", "remove obsolete wheel rotation method")

screen_path.write_text(s, encoding="utf-8")

for relative in (
    "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha52AlignmentRegressionTest.java",
    "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha53PanelBoundsRegressionTest.java",
):
    path = root / relative
    if path.exists():
        text = path.read_text(encoding="utf-8")
        text = text.replace('source.contains("0.398D - 0.108D * eased")', 'source.contains("wheelSize * 0.398D")')
        text = text.replace("wheelLabelsAndBallShareTheInnerPocketBand", "wheelLabelsStayCenteredAndBallUsesBrownTrack")
        text = text.replace("rouletteNumbersAndBallUseTheSameInnerPocketBand", "rouletteNumbersStayCenteredAndBallUsesBrownTrack")
        path.write_text(text, encoding="utf-8")

alpha51_regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha51SingleLayerRegressionTest.java"
if alpha51_regression.exists():
    text = alpha51_regression.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "void wheelAndBallSettleOnTheSharedServerResult()",
        "void staticWheelAndBallUseTheSharedServerResult()",
        "historical alpha.51 animation test name",
    )
    text = replace_once(
        text,
        'assertTrue(source.contains("TAU * 7.0D + target"));',
        'assertTrue(source.contains("double resultAngle = TAU * resultIndex / ROULETTE_WHEEL.length"));',
        "historical alpha.51 server result animation",
    )
    alpha51_regression.write_text(text, encoding="utf-8")

alpha53_regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha53PanelBoundsRegressionTest.java"
if alpha53_regression.exists():
    text = alpha53_regression.read_text(encoding="utf-8")
    text = replace_once(text, 'source.contains("refY(526)")', 'source.contains("refY(503)")', "historical alpha.53 ticket bound")
    alpha53_regression.write_text(text, encoding="utf-8")

alpha54_regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha54CompositionRegressionTest.java"
if alpha54_regression.exists():
    text = alpha54_regression.read_text(encoding="utf-8")
    text = replace_once(text, 'source.contains("refX(1245)")', 'source.contains("refX(1262)")', "historical alpha.54 balance center")
    alpha54_regression.write_text(text, encoding="utf-8")

release_dir = root / "release/0.4.0-alpha.55"
release_dir.mkdir(parents=True, exist_ok=True)
for name in ("CAMBIOS-0.4.0-alpha.55.md", "GUIA-INSTALACION-Y-PRUEBAS.md", "GUIA-PRUEBA-VISUAL.md"):
    shutil.copyfile(ci / "alpha55" / name, release_dir / name)

regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha55BallOnlyRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha55BallOnlyRegressionTest {
    private static String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void wheelAndCenterStayStaticWhileBallTargetsServerResult() throws Exception {
        String source = screen();
        assertFalse(source.contains("rouletteWheelRotation()"));
        assertFalse(source.contains("rotation((float)rotation)"));
        assertTrue(source.contains("double radius = wheelSize * 0.398D"));
        assertTrue(source.contains("ROULETTE_WHEEL[i] == result"));
        assertTrue(source.contains("double resultAngle = TAU * resultIndex / ROULETTE_WHEEL.length"));
    }

    @Test
    void balanceAndTicketTextStayInsideTheirImageWells() throws Exception {
        String source = screen();
        assertTrue(source.contains("refX(1262)"));
        assertTrue(source.contains("refX(145)"));
        assertTrue(source.contains("refX(1182)"));
        assertTrue(source.contains("refY(520)"));
        assertTrue(source.contains("refY(544)"));
    }
}
''', encoding="utf-8")

print("alpha.55 static wheel, brown-track ball and adaptive text installed")
