from pathlib import Path

root = Path('.')

# Fabric/Yarn 1.21.1 TextFieldWidget has no setTextShadow API. Keep the transparent
# wager field from the asset-backed layout and remove only that unsupported cosmetic call.
screen_path = root / 'src/client/java/com/emipokemon/client/casino/CasinoScreen.java'
screen_source = screen_path.read_text().replace('        amountField.setTextShadow(true);\n', '')
screen_path.write_text(screen_source)

# Advance inherited version checks.
for p in (root / 'src/test/java').rglob('*.java'):
    s = p.read_text()
    s = s.replace('0.4.0-alpha.45', '0.4.0-alpha.46')
    s = s.replace('alpha45VersionIsConsistent', 'alpha46VersionIsConsistent')
    p.write_text(s)

p = root / 'src/test/java/com/emipokemon/casino/CasinoRoulettePresentationRegressionTest.java'
p.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CasinoRoulettePresentationRegressionTest {
    private String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    private String server() throws Exception {
        return Files.readString(Path.of("src/main/java/com/emipokemon/casino/CasinoTableService.java"));
    }

    private String networking() throws Exception {
        return Files.readString(Path.of("src/main/java/com/emipokemon/casino/CasinoNetworking.java"));
    }

    @Test
    void approvedRouletteUsesAssetBackedChromeAndRealMappedHitboxes() throws Exception {
        String s = screen();
        assertTrue(s.contains("ROULETTE_HEADER"));
        assertTrue(s.contains("ROULETTE_LEFT_PANEL"));
        assertTrue(s.contains("ROULETTE_SIDE_PANEL"));
        assertTrue(s.contains("ROULETTE_WHEEL_OUTER"));
        assertTrue(s.contains("ROULETTE_MEDALLION"));
        assertTrue(s.contains("drawAsset(context, ROULETTE_WHEEL_OUTER"));
        assertTrue(s.contains("drawAsset(context, ROULETTE_MEDALLION"));
        assertTrue(s.contains("buildRouletteCells"));
        assertTrue(s.contains("leftPx("));
        assertTrue(s.contains("sidePy("));
        assertTrue(s.contains("send(cell.action)"));
    }

    @Test
    void rouletteAssetsExistAndAreNonEmpty() throws Exception {
        Path base = Path.of("src/main/resources/assets/emipokemon/textures/gui/casino");
        for (String name : new String[]{
                "roulette_header.png",
                "roulette_left_panel.png",
                "roulette_side_panel.png",
                "roulette_wheel_outer.png",
                "roulette_medallion.png"}) {
            Path asset = base.resolve(name);
            assertTrue(Files.isRegularFile(asset), "missing " + name);
            assertTrue(Files.size(asset) > 1024, "asset unexpectedly tiny " + name);
        }
    }

    @Test
    void resultHistoryIsRealServerBackedStateRatherThanDecoration() throws Exception {
        String server = server();
        String networking = networking();
        String screen = screen();
        assertTrue(server.contains("rouletteHistory.addFirst(number)"));
        assertTrue(server.contains("rouletteHistory.size() > 5"));
        assertTrue(server.contains("List.copyOf(session.rouletteHistory)"));
        assertTrue(networking.contains("List<Integer> recentResults"));
        assertTrue(screen.contains("state.recentResults()"));
    }

    @Test
    void quickChipsStillControlTheRealWagerFieldAndBetsStayAuthoritative() throws Exception {
        String s = screen();
        assertTrue(s.contains("QuickChipZone"));
        assertTrue(s.contains("setQuickAmount(chip.multiplier)"));
        assertTrue(s.contains("amountField.setText(Long.toString(amount))"));
        assertTrue(s.contains("amountField.setDrawsBackground(false)"));
        assertTrue(s.contains("send(cell.action)"));
    }

    @Test
    void alpha46StillKeepsNoBlurContract() throws Exception {
        String s = screen();
        assertTrue(s.contains("public void renderBackground(DrawContext context"));
        assertTrue(s.contains("Deliberately empty: the casino draws its own dim backdrop"));
        assertFalse(s.contains("        renderBackground(context, mouseX, mouseY, delta);"));
    }
}
''')
