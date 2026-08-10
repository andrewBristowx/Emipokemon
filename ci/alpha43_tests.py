from pathlib import Path

root=Path('.')

# Advance current-version assertions across inherited regression tests without weakening behavior checks.
for p in (root/'src/test/java').rglob('*.java'):
    s=p.read_text()
    s=s.replace('0.4.0-alpha.42','0.4.0-alpha.43')
    s=s.replace('alpha42VersionIsConsistentInSource','alpha43VersionIsConsistentInSource')
    s=s.replace('alpha42VersionIsConsistent','alpha43VersionIsConsistent')
    p.write_text(s)

# Alpha.37's no-blur regression encoded exact old implementation details. Keep the behavior
# requirement, but allow alpha.43's game-first renderer to satisfy it with a custom backdrop.
p=root/'src/test/java/com/emipokemon/casino/CasinoVisualRegressionTest.java'
if p.exists():
    s=p.read_text()
    old='''        assertTrue(screen.contains("public void renderBackground(DrawContext context"));\n        assertTrue(screen.contains("context.fill(0, 0, width, height, 0x99000000)"));\n        assertFalse(screen.contains("        renderBackground(context, mouseX, mouseY, delta);"));\n        assertTrue(screen.contains("private static final int PANEL = 0xFF160B1E"));\n        assertTrue(screen.contains("private static final int WHITE = 0xFFFFFFFF"));'''
    new='''        assertFalse(screen.contains("renderBackground(context"), "casino GUI must never blur its own rendered panel");\n        assertTrue(screen.contains("context.fill(0, 0, width, height, BACKDROP)"));\n        assertTrue(screen.contains("private static final int BACKDROP"));\n        assertTrue(screen.contains("private static final int PANEL"));\n        assertTrue(screen.contains("private static final int WHITE"));'''
    if old not in s:
        raise SystemExit('missing alpha43 test anchor: alpha37 no-blur contract')
    p.write_text(s.replace(old,new,1))

p=root/'src/test/java/com/emipokemon/casino/CasinoRouletteGuiRegressionTest.java'
p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(r'''package com.emipokemon.casino;

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
        assertTrue(s.contains("Haz clic directamente sobre el tapete"));
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
    void wheelSettlesOnTheSharedServerResult() throws Exception {
        String s=screen();
        assertTrue(s.contains("rouletteResultNumber"));
        assertTrue(s.contains("state.tableState()"));
        assertTrue(s.contains("\"result\".equals(state.phase())"));
        assertTrue(s.contains("1800L"));
    }

    @Test
    void casinoScreenKeepsTheNoBlurRenderingContract() throws Exception {
        String s=screen();
        assertFalse(s.contains("renderBackground(context"),"casino GUI must not blur itself in Cobbleverse");
    }

    @Test
    void alpha43VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.43"));
        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java")).contains("0.4.0-alpha.43"));
    }
}
''')
