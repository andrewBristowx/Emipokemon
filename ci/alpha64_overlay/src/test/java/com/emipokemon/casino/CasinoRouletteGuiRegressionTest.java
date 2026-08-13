package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CasinoRouletteGuiRegressionTest {
    private String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    private String tableService() throws Exception {
        return Files.readString(Path.of("src/main/java/com/emipokemon/casino/CasinoTableService.java"));
    }

    @Test
    void rouletteIsAClickableGameBoardInsteadOfAButtonList() throws Exception {
        String s=screen();
        assertTrue(s.contains("ROULETTE_WHEEL"));
        assertTrue(s.contains("buildRouletteCells"));
        assertTrue(s.contains("drawRouletteWheel"));
        assertTrue(s.contains("mouseClicked"));
        assertTrue(s.contains("number:"));
        assertTrue(s.contains("Selecciona una casilla"));
        assertFalse(s.contains("Haz clic directamente sobre el tapete"));
    }

    @Test
    void rouletteOffersCompleteOutsideDozenAndColumnBets() throws Exception {
        String s=screen();
        for (String action : new String[]{"red","black","even","odd","low","high","dozen1","dozen2","dozen3","column1","column2","column3"}) {
            assertTrue(s.contains("\""+action+"\""),"missing GUI action "+action);
        }
        String server=tableService();
        assertTrue(server.contains("column1"));
        assertTrue(server.contains("column2"));
        assertTrue(server.contains("column3"));
        assertTrue(server.contains("participant.action.startsWith(\"column\")"));
    }

    @Test
    void selectedBetIsRenderedAsAChipOnTheCloth() throws Exception {
        String s=screen();
        assertTrue(s.contains("selectedAction()"));
        assertTrue(s.contains("drawChip"));
        assertTrue(s.contains("Tu apuesta: "));
    }

    @Test
    void sharedServerResultRemainsVisibleInRecentResults() throws Exception {
        String s=screen();
        assertTrue(s.contains("rouletteResultNumber"));
        assertTrue(s.contains("state.tableState()"));
        assertTrue(s.contains("state.recentResults()"));
        assertTrue(s.contains("drawRecentResults(context)"));
    }

    @Test
    void casinoScreenKeepsTheNoBlurRenderingContract() throws Exception {
        String s=screen();
        assertFalse(s.contains("        renderBackground(context, mouseX, mouseY, delta);"),
                "casino render must not invoke vanilla blur directly");
        assertTrue(s.contains("public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)"),
                "casino screen must override the inherited background hook");
        assertTrue(s.contains("Deliberately empty: the casino draws its own dim backdrop"),
                "the inherited background hook must remain an intentional no-op");
        assertTrue(s.contains("context.fill(0, 0, width, height, BACKDROP)"),
                "casino must draw a deterministic dim backdrop itself");
    }

    @Test
    void alpha60VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.64"));
        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java")).contains("0.4.0-alpha.64"));
    }
}
