from pathlib import Path
root=Path('.')

for p in (root/'src/test/java').rglob('*.java'):
    s=p.read_text()
    s=s.replace('0.4.0-alpha.44','0.4.0-alpha.45')
    s=s.replace('alpha44VersionIsConsistentInSource','alpha45VersionIsConsistentInSource')
    s=s.replace('alpha44VersionIsConsistent','alpha45VersionIsConsistent')
    p.write_text(s)

p=root/'src/test/java/com/emipokemon/casino/CasinoRouletteGuiRegressionTest.java'
s=p.read_text()
s=s.replace('assertTrue(s.contains("Haz clic directamente sobre el tapete"));',
            'assertTrue(s.contains("Selecciona una casilla"));\n        assertFalse(s.contains("Haz clic directamente sobre el tapete"));')
p.write_text(s)

(root/'src/test/java/com/emipokemon/casino/CasinoRoulettePresentationRegressionTest.java').write_text(r'''package com.emipokemon.casino;

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
    void approvedRouletteLayoutKeepsWheelClearAndUsesCompactHelp() throws Exception {
        String s = screen();
        assertTrue(s.contains("wheelCy = contentTop + 84"));
        assertTrue(s.contains("rouletteBoardY = contentTop + 164"));
        assertTrue(s.contains("Selecciona una casilla"));
        assertFalse(s.contains("Haz clic directamente sobre el tapete para colocar tu ficha"));
        assertTrue(s.contains("drawRecentResults(context)"));
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
    void bottomCasinoChipsControlTheRealWagerField() throws Exception {
        String s = screen();
        assertTrue(s.contains("QuickChipZone"));
        assertTrue(s.contains("setQuickAmount(chip.multiplier)"));
        assertTrue(s.contains("amountField.setText(Long.toString(amount))"));
        assertTrue(s.contains("send(cell.action)"));
    }

    @Test
    void pokemonWorldDecorationUsesLocalDrawPrimitivesOnly() throws Exception {
        String s = screen();
        assertTrue(s.contains("drawCaptureBall"));
        assertTrue(s.contains("drawCasinoChip"));
        assertTrue(s.contains("drawCoinStack"));
        assertFalse(s.contains("textures/gui/casino_roulette"));
    }

    @Test
    void alpha45StillKeepsNoBlurContract() throws Exception {
        String s = screen();
        assertTrue(s.contains("public void renderBackground(DrawContext context"));
        assertTrue(s.contains("Deliberately empty: the casino draws its own dim backdrop"));
        assertFalse(s.contains("        renderBackground(context, mouseX, mouseY, delta);"));
    }
}
''')
