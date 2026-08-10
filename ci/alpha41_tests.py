from pathlib import Path

root=Path('.')

# Advance inherited alpha.40 version assertions while preserving every multiplayer/model invariant.
for rel in [
 'src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoMultiplayerRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoVisualRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoSilhouetteRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoMaterialScaleRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoConstructionDetailRegressionTest.java',
]:
    p=root/rel
    if p.exists():
        s=p.read_text().replace('0.4.0-alpha.40','0.4.0-alpha.41')
        s=s.replace('alpha40VersionIsConsistentInSource','alpha41VersionIsConsistentInSource')
        s=s.replace('alpha40VersionIsConsistent','alpha41VersionIsConsistent')
        s=s.replace('alpha40VerticalCabinetsAreEssentiallyTwoBlocksTall','alpha41VerticalCabinetsAreEssentiallyTwoBlocksTall')
        s=s.replace('alpha40TablesStayAtFurnitureHeight','alpha41TablesStayAtFurnitureHeight')
        s=s.replace('alpha40RendererSupportsTallGeometry','alpha41RendererSupportsTallGeometry')
        s=s.replace('alpha40TexturesContainMaterialVariation','alpha41TexturesContainMaterialVariation')
        p.write_text(s)

p=root/'src/test/java/com/emipokemon/casino/CasinoGuiRegressionTest.java'
p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(r'''package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CasinoGuiRegressionTest {
    private String screen() throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/CasinoScreen.java"));
    }

    @Test
    void casinoScreenKeepsManualNoBlurBackdrop() throws Exception {
        String s=screen();
        assertTrue(s.contains("context.fill(0, 0, width, height, BACKDROP)"));
        assertTrue(s.contains("public void renderBackground(DrawContext context"));
        assertFalse(s.contains("renderBackground(context, mouseX, mouseY, delta);"));
    }

    @Test
    void sharedGamesAreExplicitlyPresentedAsMultiplayer() throws Exception {
        String s=screen();
        assertTrue(s.contains("MESA MULTIJUGADOR"));
        assertTrue(s.contains("case \"roulette\", \"dice\", \"blackjack\", \"poker\" -> true"));
        assertTrue(s.contains("Jugadores / apuestas"));
        assertTrue(s.contains("Mesa libre: esperando jugadores"));
    }

    @Test
    void bettingControlsRemainServerDriven() throws Exception {
        String s=screen();
        assertTrue(s.contains("ClientPlayNetworking.send(new CasinoNetworking.CasinoActionPayload"));
        assertTrue(s.contains("state.minimumBet()"));
        assertTrue(s.contains("setQuickAmount(1)"));
        assertTrue(s.contains("setQuickAmount(5)"));
        assertTrue(s.contains("setQuickAmount(10)"));
        assertTrue(s.contains("number < 0 || number > 36"));
    }

    @Test
    void interfaceHasGameSpecificIdentityAndInformationHierarchy() throws Exception {
        String s=screen();
        assertTrue(s.contains("APUESTA / ACCIÓN"));
        assertTrue(s.contains("ESTADO DE LA MESA"));
        assertTrue(s.contains("Tu estado"));
        assertTrue(s.contains("private int gameAccent()"));
        assertTrue(s.contains("private String gameHint()"));
        assertTrue(s.contains("CASINO"));
    }

    @Test
    void alpha41VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.41"));
        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java")).contains("0.4.0-alpha.41"));
    }
}
''')
