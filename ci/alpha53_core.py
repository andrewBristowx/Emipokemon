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
    if "0.4.0-alpha.52" not in text:
        raise AssertionError(f"missing alpha.52 marker in {relative}")
    path.write_text(text.replace("0.4.0-alpha.52", "0.4.0-alpha.53"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.52", "0.4.0-alpha.53")
    text = text.replace("alpha52VersionIsConsistent", "alpha53VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

screen_path = root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java"
s = screen_path.read_text(encoding="utf-8")

old_side_content = '''        String selectedAmount = selectedBetAmount();
        String own = selected.isBlank() ? "AÚN NO HAS APOSTADO"
                : betLabel(selected).toUpperCase(java.util.Locale.ROOT) + (selectedAmount.isBlank() ? "" : " · " + selectedAmount);
        drawIntegratedWrapped(context, own, sideX + sidePx(35), contentTop + sidePy(266), sidePx(235), WHITE, contentTop + sidePy(307));
        if (selected.isBlank()) {
            drawUiText(context, "Selecciona una casilla en el tapete", sideX + sidePx(35), contentTop + sidePy(291), MUTED, false);
        }

        List<String> players = state.players() == null ? List.of() : state.players();
        renderPlayerCount(context, players.size());
        drawPlayers(context, contentTop + sidePy(365), contentTop + sidePy(449));

        drawIntegratedWrapped(context, safe(state.tableState(), "Mesa lista para una nueva ronda."),
                sideX + sidePx(33), contentTop + sidePy(507), sidePx(222), WHITE, contentTop + sidePy(548));'''
new_side_content = '''        String selectedAmount = selectedBetAmount();
        String own = selected.isBlank() ? "AÚN NO HAS APOSTADO"
                : betLabel(selected).toUpperCase(java.util.Locale.ROOT) + (selectedAmount.isBlank() ? "" : " · " + selectedAmount);
        context.enableScissor(panelX + refX(1125), panelY + refY(526), panelX + refX(1494), panelY + refY(589));
        drawScaledIntegratedWrapped(context, own, panelX + refX(1143), panelY + refY(535),
                refX(337), WHITE, panelY + refY(559), 0.82F);
        if (selected.isBlank()) {
            drawScaledIntegratedWrapped(context, "Selecciona una casilla en el tapete",
                    panelX + refX(1143), panelY + refY(561), refX(337), MUTED, panelY + refY(585), 0.76F);
        }
        context.disableScissor();

        List<String> players = state.players() == null ? List.of() : state.players();
        renderPlayerCount(context, players.size());
        context.enableScissor(panelX + refX(1124), panelY + refY(666), panelX + refX(1495), panelY + refY(790));
        drawPlayers(context, contentTop + sidePy(365), contentTop + sidePy(449));
        context.disableScissor();

        context.enableScissor(panelX + refX(1127), panelY + refY(842), panelX + refX(1492), panelY + refY(929));
        drawScaledIntegratedWrapped(context, safe(state.tableState(), "Mesa lista para una nueva ronda."),
                panelX + refX(1172), panelY + refY(865), refX(292), WHITE, panelY + refY(918), 0.76F);
        context.disableScissor();'''
s = replace_once(s, old_side_content, new_side_content, "bounded side panel content")

old_helper_end = '''        return y;
    }

    private void drawRouletteWheel'''
new_helper_end = '''        return y;
    }

    private void drawScaledIntegratedWrapped(DrawContext context, String value, int x, int y,
                                             int width, int color, int maxY, float scale) {
        if (value == null || value.isBlank()) return;
        int wrapWidth = Math.max(1, Math.round(width / scale));
        int localMaxY = Math.max(0, Math.round((maxY - y) / scale));
        int localY = 0;
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        for (var line : textRenderer.wrapLines(casinoText(value, false), wrapWidth)) {
            if (localY + 9 > localMaxY) break;
            context.drawTextWithShadow(textRenderer, line, 0, localY, color);
            localY += 11;
        }
        context.getMatrices().pop();
    }

    private void drawRouletteWheel'''
s = replace_once(s, old_helper_end, new_helper_end, "scaled bounded text helper")

s = replace_once(
    s,
    '''        float numberScale = Math.max(0.62F, Math.min(0.76F, size / 438.0F * 0.82F));
        int numberRadius = Math.round(size * 0.334F);''',
    '''        float numberScale = Math.max(0.72F, Math.min(0.86F, size / 438.0F * 0.92F));
        int numberRadius = Math.round(size * 0.286F);''',
    "centered pocket labels",
)
s = replace_once(
    s,
    '''        double radius = wheelSize * (0.456D - 0.118D * eased);''',
    '''        double radius = wheelSize * (0.398D - 0.108D * eased);''',
    "ball groove alignment",
)
screen_path.write_text(s, encoding="utf-8")

alpha52_regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha52AlignmentRegressionTest.java"
if alpha52_regression.exists():
    text = alpha52_regression.read_text(encoding="utf-8")
    text = replace_once(text, "size * 0.334F", "size * 0.286F", "historical alpha.52 number band")
    text = replace_once(text, "0.456D - 0.118D * eased", "0.398D - 0.108D * eased", "historical alpha.52 ball band")
    alpha52_regression.write_text(text, encoding="utf-8")

release_dir = root / "release/0.4.0-alpha.53"
release_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(ci / "alpha53/CAMBIOS-0.4.0-alpha.53.md", release_dir / "CAMBIOS-0.4.0-alpha.53.md")
shutil.copyfile(ci / "alpha53/GUIA-PRUEBA-VISUAL.md", release_dir / "GUIA-PRUEBA-VISUAL.md")

regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha53PanelBoundsRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha53PanelBoundsRegressionTest {
    private static String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void rouletteNumbersAndBallUseTheSameInnerPocketBand() throws Exception {
        String source = screen();
        assertTrue(source.contains("size * 0.286F"));
        assertTrue(source.contains("0.398D - 0.108D * eased"));
        assertTrue(source.contains("rouletteResultNumber()"));
    }

    @Test
    void everyVariableSideSectionHasHardVisualBounds() throws Exception {
        String source = screen();
        assertTrue(source.contains("refY(526)"));
        assertTrue(source.contains("refY(790)"));
        assertTrue(source.contains("refY(842)"));
        assertTrue(source.contains("refX(1172)"));
        assertTrue(source.contains("drawScaledIntegratedWrapped"));
        assertTrue(source.contains("context.disableScissor()"));
    }
}
''', encoding="utf-8")

print("alpha.53 inner wheel band and bounded side panels installed")
