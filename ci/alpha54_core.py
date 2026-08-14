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
    if "0.4.0-alpha.53" not in text:
        raise AssertionError(f"missing alpha.53 marker in {relative}")
    path.write_text(text.replace("0.4.0-alpha.53", "0.4.0-alpha.54"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.53", "0.4.0-alpha.54")
    text = text.replace("alpha53VersionIsConsistent", "alpha54VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

screen_path = root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java"
s = screen_path.read_text(encoding="utf-8")

s = replace_once(
    s,
    '''        context.enableScissor(panelX + refX(1125), panelY + refY(526), panelX + refX(1494), panelY + refY(589));
        drawScaledIntegratedWrapped(context, own, panelX + refX(1143), panelY + refY(535),
                refX(337), WHITE, panelY + refY(559), 0.82F);
        if (selected.isBlank()) {
            drawScaledIntegratedWrapped(context, "Selecciona una casilla en el tapete",
                    panelX + refX(1143), panelY + refY(561), refX(337), MUTED, panelY + refY(585), 0.76F);
        }''',
    '''        context.enableScissor(panelX + refX(1125), panelY + refY(526), panelX + refX(1490), panelY + refY(589));
        drawScaledIntegratedWrapped(context, own, panelX + refX(1194), panelY + refY(541),
                refX(280), WHITE, panelY + refY(561), 0.82F);
        if (selected.isBlank()) {
            drawScaledIntegratedWrapped(context, "Selecciona una casilla en el tapete",
                    panelX + refX(1194), panelY + refY(563), refX(280), MUTED, panelY + refY(586), 0.74F);
        }''',
    "tu ficha text lane",
)

s = replace_once(
    s,
    '''        context.enableScissor(panelX + refX(1127), panelY + refY(842), panelX + refX(1492), panelY + refY(929));
        drawScaledIntegratedWrapped(context, safe(state.tableState(), "Mesa lista para una nueva ronda."),
                panelX + refX(1172), panelY + refY(865), refX(292), WHITE, panelY + refY(918), 0.76F);''',
    '''        context.enableScissor(panelX + refX(1127), panelY + refY(842), panelX + refX(1418), panelY + refY(929));
        drawScaledIntegratedWrapped(context, safe(state.tableState(), "Mesa lista para una nueva ronda."),
                panelX + refX(1194), panelY + refY(865), refX(218), WHITE, panelY + refY(918), 0.72F);''',
    "mesa text lane",
)

s = replace_once(
    s,
    '''        int x = panelX + headerPx(1176);
        int w = Math.max(76, headerPx(172));
        int y = panelY + headerPy(72);
        String value = state.balance() + " Michicoins";
        drawFittedCenteredUiText(context, value, x + w / 2, y, w - headerPx(12), 0xFFFFDF62, false);''',
    '''        int centerX = panelX + refX(1245);
        int y = panelY + refY(76);
        String value = state.balance() + " Michicoins";
        drawFittedCenteredUiText(context, value, centerX, y, refX(172), 0xFFFFDF62, false);''',
    "centered balance capsule",
)

s = replace_once(
    s,
    '''        int x = panelX + refX(1432);
        int y = panelY + refY(657);
        int w = refX(65);
        String value = count + "/" + ROULETTE_DISPLAY_CAPACITY;
        drawFittedCenteredUiText(context, value, x + w / 2, y + refY(7),
                w - refX(8), 0xFFFFD85B, false);''',
    '''        int x = panelX + refX(1424);
        int y = panelY + refY(600);
        int w = refX(52);
        String value = count + "/" + ROULETTE_DISPLAY_CAPACITY;
        drawFittedCenteredUiText(context, value, x + w / 2, y + refY(8),
                w - refX(6), 0xFFFFD85B, false);''',
    "player count header lane",
)

s = replace_once(
    s,
    '''        // The logo remains readable while the mechanical wheel underneath it rotates.
        int medSize = refX(158);
        drawAsset(context, ROULETTE_MEDALLION, wheelCx - medSize / 2, wheelCy - medSize / 2, medSize, medSize, ROULETTE_MEDALLION_TEX_SIZE, ROULETTE_MEDALLION_TEX_SIZE);
        drawRouletteBall(context, size);''',
    '''        // The wheel texture owns its center art; a second static medallion caused a duplicated logo.
        drawRouletteBall(context, size);''',
    "remove duplicated static medallion",
)

screen_path.write_text(s, encoding="utf-8")

alpha53_regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha53PanelBoundsRegressionTest.java"
if alpha53_regression.exists():
    text = alpha53_regression.read_text(encoding="utf-8")
    text = replace_once(text, 'source.contains("refX(1172)")', 'source.contains("refX(1194)")', "historical alpha.53 mesa anchor")
    alpha53_regression.write_text(text, encoding="utf-8")

presentation = root / "src/test/java/com/emipokemon/casino/CasinoRoulettePresentationRegressionTest.java"
if presentation.exists():
    text = presentation.read_text(encoding="utf-8")
    text = replace_once(
        text,
        'assertTrue(s.contains("drawAsset(context, ROULETTE_MEDALLION"));',
        'assertFalse(s.contains("drawAsset(context, ROULETTE_MEDALLION"));',
        "single center layer contract",
    )
    presentation.write_text(text, encoding="utf-8")

alpha52_alignment = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha52AlignmentRegressionTest.java"
if alpha52_alignment.exists():
    text = alpha52_alignment.read_text(encoding="utf-8")
    text = replace_once(
        text,
        'assertTrue(source.contains("refX(158)"));',
        'assertFalse(source.contains("drawAsset(context, ROULETTE_MEDALLION"));',
        "historical alpha.52 center-layer contract",
    )
    text = replace_once(
        text,
        'import static org.junit.jupiter.api.Assertions.assertTrue;',
        'import static org.junit.jupiter.api.Assertions.assertFalse;\nimport static org.junit.jupiter.api.Assertions.assertTrue;',
        "historical alpha.52 false assertion import",
    )
    alpha52_alignment.write_text(text, encoding="utf-8")

release_dir = root / "release/0.4.0-alpha.54"
release_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(ci / "alpha54/CAMBIOS-0.4.0-alpha.54.md", release_dir / "CAMBIOS-0.4.0-alpha.54.md")
shutil.copyfile(ci / "alpha54/GUIA-PRUEBA-VISUAL.md", release_dir / "GUIA-PRUEBA-VISUAL.md")
shutil.copyfile(ci / "alpha54/GUIA-INSTALACION-Y-PRUEBAS.md", release_dir / "GUIA-INSTALACION-Y-PRUEBAS.md")

regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha54CompositionRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoRouletteAlpha54CompositionRegressionTest {
    private static String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void wheelHasNoSecondStaticCenterLogo() throws Exception {
        String source = screen();
        assertTrue(source.contains("drawAsset(context, ROULETTE_ALPHA51_WHEEL"));
        assertFalse(source.contains("drawAsset(context, ROULETTE_MEDALLION"));
    }

    @Test
    void variableTextUsesReservedImageLanes() throws Exception {
        String source = screen();
        assertTrue(source.contains("refX(1245)"));
        assertTrue(source.contains("refX(1194)"));
        assertTrue(source.contains("refX(1424)"));
        assertTrue(source.contains("refY(600)"));
        assertTrue(source.contains("refX(1418)"));
        assertTrue(source.contains("refX(218)"));
    }
}
''', encoding="utf-8")

print("alpha.54 single-layer wheel and bounded text lanes installed")
