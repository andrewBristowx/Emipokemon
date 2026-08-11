from pathlib import Path
import shutil


root = Path(".")
script_root = Path(__file__).resolve().parent


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise AssertionError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


# Advance only the reconstructed alpha.49 candidate; alpha.49 itself remains immutable.
for relative in ("gradle.properties", "src/main/java/com/emipokemon/Emipokemon.java"):
    path = root / relative
    text = path.read_text(encoding="utf-8")
    if "0.4.0-alpha.49" not in text:
        raise AssertionError(f"missing alpha.49 marker in {relative}")
    path.write_text(text.replace("0.4.0-alpha.49", "0.4.0-alpha.50"), encoding="utf-8")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.49", "0.4.0-alpha.50")
    text = text.replace("alpha49VersionIsConsistent", "alpha50VersionIsConsistent")
    test.write_text(text, encoding="utf-8")

# Update two historical visual contracts whose alpha.49 implementation is intentionally
# superseded. They continue to verify server-backed results and reference-aligned layout.
gui_test = root / "src/test/java/com/emipokemon/casino/CasinoRouletteGuiRegressionTest.java"
text = gui_test.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    void wheelSettlesOnTheSharedServerResult() throws Exception {
        String s=screen();
        assertTrue(s.contains("rouletteResultNumber"));
        assertTrue(s.contains("state.tableState()"));
        assertTrue(s.contains("\\\"result\\\".equals(state.phase())"));
        assertTrue(s.contains("1800L"));
    }''',
    '''    void sharedServerResultRemainsVisibleInRecentResults() throws Exception {
        String s=screen();
        assertTrue(s.contains("rouletteResultNumber"));
        assertTrue(s.contains("state.tableState()"));
        assertTrue(s.contains("state.recentResults()"));
        assertTrue(s.contains("drawRecentResults(context)"));
    }''',
    "historical wheel-result regression",
)
gui_test.write_text(text, encoding="utf-8")

integrated_test = root / "src/test/java/com/emipokemon/casino/CasinoRouletteIntegratedUiRegressionTest.java"
text = integrated_test.read_text(encoding="utf-8")
text = replace_once(
    text,
    '        assertTrue(s.contains("rouletteContentH = panelY + panelH - contentTop"));',
    '        assertTrue(s.contains("ROULETTE_LEFT_TEX_H / (float)ROULETTE_REFERENCE_H"));',
    "historical content-height regression",
)
integrated_test.write_text(text, encoding="utf-8")

# Preserve the exact user-provided 1535x1024 reference. It is rendered underneath the
# clean split textures so only the missing divider/footer artwork remains visible.
asset_source = script_root / "assets" / "roulette_reference.png"
asset_target = root / "src/main/resources/assets/emipokemon/textures/gui/casino/roulette_reference.png"
asset_target.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(asset_source, asset_target)

screen_path = root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java"
s = screen_path.read_text(encoding="utf-8")

s = replace_once(
    s,
    '    private static final Identifier ROULETTE_MEDALLION = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/roulette_medallion.png");\n',
    '    private static final Identifier ROULETTE_MEDALLION = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/roulette_medallion.png");\n'
    '    private static final Identifier ROULETTE_REFERENCE = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/roulette_reference.png");\n',
    "reference identifier",
)
s = replace_once(
    s,
    '    private static final int ROULETTE_DISPLAY_CAPACITY = 8;\n',
    '    private static final int ROULETTE_DISPLAY_CAPACITY = 8;\n'
    '    private static final int ROULETTE_REFERENCE_W = 1535;\n'
    '    private static final int ROULETTE_REFERENCE_H = 1024;\n'
    '    private static final int ROULETTE_CONTENT_SOURCE_Y = 146;\n'
    '    private static final int ROULETTE_SIDE_SOURCE_X = 1095;\n',
    "reference dimensions",
)

old_init = '''        rouletteCells.clear();
        quickChipZones.clear();
        panelW = Math.min(1080, Math.max(620, width - 20));
        panelH = Math.min(660, Math.max(430, height - 20));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        if (isRoulette()) {
            rouletteHeaderH = Math.max(52, Math.min(ROULETTE_HEADER_H, Math.round(panelW * (ROULETTE_HEADER_TEX_H / (float)ROULETTE_HEADER_TEX_W))));
            contentTop = panelY + rouletteHeaderH;
            initRoulette();
        } else {'''
new_init = '''        rouletteCells.clear();
        quickChipZones.clear();

        if (isRoulette()) {
            // Fit the original reference composition by both axes. Independent width/height scaling
            // distorted alpha.49, enlarged the wheel and cut the footer on wide displays.
            int availableW = Math.min(ROULETTE_REFERENCE_W, Math.max(1, width - 24));
            int availableH = Math.min(ROULETTE_REFERENCE_H, Math.max(1, height - 24));
            float referenceAspect = ROULETTE_REFERENCE_W / (float)ROULETTE_REFERENCE_H;
            if (availableW / (float)availableH > referenceAspect) {
                panelH = availableH;
                panelW = Math.max(1, Math.round(panelH * referenceAspect));
            } else {
                panelW = availableW;
                panelH = Math.max(1, Math.round(panelW / referenceAspect));
            }
        } else {
            panelW = Math.min(1080, Math.max(620, width - 20));
            panelH = Math.min(660, Math.max(430, height - 20));
        }
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        if (isRoulette()) {
            rouletteHeaderH = Math.max(1, Math.round(panelH * (ROULETTE_HEADER_TEX_H / (float)ROULETTE_REFERENCE_H)));
            contentTop = panelY + rouletteHeaderH;
            initRoulette();
        } else {'''
s = replace_once(s, old_init, new_init, "aspect-fit initialization")

old_layout = '''        // The HD source is one composition split into a header and two content textures.
        // Fill the panel width so wide GUI scales never create artificial black side gutters.
        int availableContentH = panelH - rouletteHeaderH;
        rouletteOverlapH = Math.max(5, Math.round(availableContentH * 28.0F / ROULETTE_LEFT_TEX_H));
        contentTop -= rouletteOverlapH;
        rouletteContentH = panelY + panelH - contentTop;
        gameX = panelX;
        gameW = Math.round(panelW * (ROULETTE_LEFT_TEX_W / (float)(ROULETTE_LEFT_TEX_W + ROULETTE_SIDE_TEX_W)));
        sideX = gameX + gameW;
        sideW = panelX + panelW - sideX;
'''
new_layout = '''        // Map every split texture to its exact position in the 1535x1024 reference.
        // The untouched base supplies the original 43 px divider and 50 px footer.
        rouletteOverlapH = 0;
        contentTop = panelY + Math.round(panelH * (ROULETTE_CONTENT_SOURCE_Y / (float)ROULETTE_REFERENCE_H));
        rouletteContentH = Math.round(panelH * (ROULETTE_LEFT_TEX_H / (float)ROULETTE_REFERENCE_H));
        gameX = panelX;
        gameW = Math.round(panelW * (ROULETTE_LEFT_TEX_W / (float)ROULETTE_REFERENCE_W));
        sideX = panelX + Math.round(panelW * (ROULETTE_SIDE_SOURCE_X / (float)ROULETTE_REFERENCE_W));
        sideW = Math.round(panelW * (ROULETTE_SIDE_TEX_W / (float)ROULETTE_REFERENCE_W));
'''
s = replace_once(s, old_layout, new_layout, "reference-coordinate layout")

old_render = '''        if (isRoulette()) {
            drawAsset(context, ROULETTE_LEFT_PANEL, gameX, contentTop, gameW, rouletteContentH, ROULETTE_LEFT_TEX_W, ROULETTE_LEFT_TEX_H);
            drawAsset(context, ROULETTE_SIDE_PANEL, sideX, contentTop, sideW, rouletteContentH, ROULETTE_SIDE_TEX_W, ROULETTE_SIDE_TEX_H);
            // The source header overlaps both panels. Drawing it last hides the clipped seam
            // fragments that were visible immediately below the alpha.48 header.
            drawAsset(context, ROULETTE_HEADER, panelX, panelY, panelW, rouletteHeaderH, ROULETTE_HEADER_TEX_W, ROULETTE_HEADER_TEX_H);
'''
new_render = '''        if (isRoulette()) {
            // Exact full reference first: this restores the original footer and divider instead
            // of fabricating borders or leaving cut ornaments at the viewport edge.
            drawAsset(context, ROULETTE_REFERENCE, panelX, panelY, panelW, panelH,
                    ROULETTE_REFERENCE_W, ROULETTE_REFERENCE_H);
            drawAsset(context, ROULETTE_LEFT_PANEL, gameX, contentTop, gameW, rouletteContentH,
                    ROULETTE_LEFT_TEX_W, ROULETTE_LEFT_TEX_H);
            drawAsset(context, ROULETTE_SIDE_PANEL, sideX, contentTop, sideW, rouletteContentH,
                    ROULETTE_SIDE_TEX_W, ROULETTE_SIDE_TEX_H);
            drawAsset(context, ROULETTE_HEADER, panelX, panelY, panelW, rouletteHeaderH,
                    ROULETTE_HEADER_TEX_W, ROULETTE_HEADER_TEX_H);
'''
s = replace_once(s, old_render, new_render, "full reference render")

old_count = '''        int x = sideX + sidePx(240);
        int y = contentTop + sidePy(333);
        int w = sidePx(40);
        int h = sidePy(17);
        // Mask only the baked glyphs, not the blue player icon or the gold section frame.
        context.fill(x, y, x + w, y + h, 0xFF53082D);
        String value = count + "/" + ROULETTE_DISPLAY_CAPACITY;
        drawCenteredUiText(context, value, x + w / 2, y + sidePy(3), 0xFFFFD85B, false);'''
new_count = '''        int x = sideX + sidePx(227);
        int y = contentTop + sidePy(329);
        int w = sidePx(61);
        int h = sidePy(23);
        // Cover the complete baked counter (including its leftmost zero) without touching the icon.
        context.fill(x, y, x + w, y + h, 0xFF53082D);
        String value = count + "/" + ROULETTE_DISPLAY_CAPACITY;
        drawFittedCenteredUiText(context, value, x + w / 2, y + sidePy(6),
                w - sidePx(8), 0xFFFFD85B, false);'''
s = replace_once(s, old_count, new_count, "complete player counter mask")

old_wheel_loop = '''        double rotation = rouletteWheelRotation();
        for (int i = 0; i < ROULETTE_WHEEL.length; i++) {
            int number = ROULETTE_WHEEL[i];
            double angle = rotation + TAU * i / ROULETTE_WHEEL.length - Math.PI / 2.0D;
            int px = wheelCx + (int)Math.round(Math.cos(angle) * wheelRadius);
            int py = wheelCy + (int)Math.round(Math.sin(angle) * wheelRadius);
            int color = number == 0 ? 0xFF07854D : RED_NUMBERS.contains(number) ? 0xFFD92735 : 0xFF171318;
            int tileW = Math.max(10, leftPx(15));
            int tileH = Math.max(8, leftPy(13));
            context.fill(px - tileW / 2, py - tileH / 2, px + (tileW + 1) / 2, py + (tileH + 1) / 2, color);
            outline(context, px - tileW / 2, py - tileH / 2, tileW, tileH, 0xFFD59A33);
            String label = Integer.toString(number);
            context.getMatrices().push();
            context.getMatrices().translate(px, py, 0.0F);
            context.getMatrices().scale(0.72F, 0.72F, 1.0F);
            context.drawTextWithShadow(textRenderer, Text.literal(label), -textRenderer.getWidth(label) / 2, -4, WHITE);
            context.getMatrices().pop();
        }
'''
new_wheel_loop = '''        // The outer texture already owns the red/black crown. Keep labels fixed to those
        // slots; alpha.49 rotated labels independently and produced overlaps/misalignment.
        int numberRadius = wheelRadius + leftPx(12);
        for (int i = 0; i < ROULETTE_WHEEL.length; i++) {
            int number = ROULETTE_WHEEL[i];
            double angle = TAU * i / ROULETTE_WHEEL.length - Math.PI / 2.0D;
            int px = wheelCx + (int)Math.round(Math.cos(angle) * numberRadius);
            int py = wheelCy + (int)Math.round(Math.sin(angle) * numberRadius);
            String label = Integer.toString(number);
            context.getMatrices().push();
            context.getMatrices().translate(px, py, 0.0F);
            context.getMatrices().scale(0.70F, 0.70F, 1.0F);
            context.drawTextWithShadow(textRenderer, Text.literal(label), -textRenderer.getWidth(label) / 2, -4, WHITE);
            context.getMatrices().pop();
        }
'''
s = replace_once(s, old_wheel_loop, new_wheel_loop, "fixed wheel labels")

old_dynamic_ball = '''        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAt);
        int result = rouletteResultNumber();
        boolean settled = "result".equals(state.phase()) && result >= 0 && elapsed >= 1800L;
        double ballAngle = settled ? -Math.PI / 2.0D : -rouletteWheelRotation() * 1.31D + 0.45D;
        int ballRadius = wheelRadius + leftPx(19);
        int bx = wheelCx + (int)Math.round(Math.cos(ballAngle) * ballRadius);
        int by = wheelCy + (int)Math.round(Math.sin(ballAngle) * ballRadius);
        drawCircle(context, bx, by, Math.max(3, leftPx(5)), WHITE);
        drawCircleOutline(context, bx, by, Math.max(3, leftPx(5)), 0xFF886C4C);
'''
new_dynamic_ball = '''        // The approved wheel texture already includes one ball. Do not draw a second one.
        // Server-authoritative outcomes remain visible in the recent-results well.
'''
s = replace_once(s, old_dynamic_ball, new_dynamic_ball, "duplicate ball removal")

screen_path.write_text(s, encoding="utf-8")

# The alpha.49 layout regression describes behavior intentionally superseded here.
(root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha49LayoutRegressionTest.java").unlink(missing_ok=True)

regression = root / "src/test/java/com/emipokemon/casino/CasinoRouletteAlpha50ViewportRegressionTest.java"
regression.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CasinoRouletteAlpha50ViewportRegressionTest {
    private String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void compositionFitsBothAxesAndRestoresExactReferenceArtwork() throws Exception {
        String s = screen();
        assertTrue(s.contains("referenceAspect"));
        assertTrue(s.contains("width - 24"));
        assertTrue(s.contains("height - 24"));
        assertTrue(s.contains("ROULETTE_REFERENCE_W = 1535"));
        assertTrue(s.contains("ROULETTE_REFERENCE_H = 1024"));
        assertTrue(s.contains("drawAsset(context, ROULETTE_REFERENCE"));
        assertTrue(s.contains("ROULETTE_SIDE_SOURCE_X / (float)ROULETTE_REFERENCE_W"));
        assertFalse(s.contains("contentTop -= rouletteOverlapH"));
    }

    @Test
    void wheelHasOneBallAndLabelsStayAlignedToStaticSlots() throws Exception {
        String s = screen();
        assertTrue(s.contains("double angle = TAU * i / ROULETTE_WHEEL.length"));
        assertFalse(s.contains("double angle = rotation + TAU"));
        assertFalse(s.contains("drawCircle(context, bx, by"));
        assertFalse(s.contains("int tileW = Math.max(10, leftPx(15))"));
    }

    @Test
    void livePlayerCounterMasksTheWholeBakedValue() throws Exception {
        String s = screen();
        assertTrue(s.contains("sidePx(227)"));
        assertTrue(s.contains("sidePx(61)"));
        assertTrue(s.contains("drawFittedCenteredUiText(context, value"));
        assertTrue(s.contains("count + \"/\" + ROULETTE_DISPLAY_CAPACITY"));
    }
}
''', encoding="utf-8")
