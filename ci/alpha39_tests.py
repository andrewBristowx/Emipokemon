from pathlib import Path

root=Path('.')

# Advance inherited version assertions.
for rel in [
 'src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoMultiplayerRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoVisualRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoSilhouetteRegressionTest.java',
]:
    p=root/rel
    if p.exists():
        s=p.read_text().replace('0.4.0-alpha.38','0.4.0-alpha.39')
        s=s.replace('alpha38VersionIsConsistentInSource','alpha39VersionIsConsistentInSource')
        s=s.replace('alpha38VersionIsConsistent','alpha39VersionIsConsistent')
        p.write_text(s)

# Alpha.36 originally required every visual cube to fit in one block. Alpha.39 intentionally
# makes the three cabinet machines ~2 blocks tall while preserving a one-block footprint.
p=root/'src/test/java/com/emipokemon/casino/CasinoMultiplayerRegressionTest.java'
if p.exists():
    s=p.read_text()
    old='assertTrue(y >= 0.0 && y + sy <= 16.0, model + " y bounds: " + cube);'
    new='double maxY = java.util.Set.of("slot", "chip_exchange", "ticket_exchange").contains(model) ? 32.0 : 16.0;\n                    assertTrue(y >= 0.0 && y + sy <= maxY, model + " y bounds: " + cube);'
    if old not in s: raise SystemExit('missing alpha39 inherited y bound assertion')
    s=s.replace(old,new,1)
    p.write_text(s)

# Alpha.37 required distinct base body colors. Alpha.39 intentionally shares material families;
# identity now lives in each machine-specific detail panel instead.
p=root/'src/test/java/com/emipokemon/casino/CasinoVisualRegressionTest.java'
if p.exists():
    s=p.read_text()
    s=s.replace('bodyColors.add(body);','bodyColors.add(image.getRGB(100, 60) & 0xFFFFFF);')
    s=s.replace('every casino machine needs a distinct body identity','every casino machine needs a distinct game-detail identity')
    p.write_text(s)

p=root/'src/test/java/com/emipokemon/casino/CasinoMaterialScaleRegressionTest.java'
p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(r'''package com.emipokemon.casino;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CasinoMaterialScaleRegressionTest {
    private double maxY(String id) throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/emipokemon/geo/casino_"+id+".geo.json"))).getAsJsonObject();
        JsonArray bones = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        double max=0;
        for (var b:bones) {
            JsonObject bone=b.getAsJsonObject();
            if (!bone.has("cubes")) continue;
            for (var ce:bone.getAsJsonArray("cubes")) {
                JsonObject c=ce.getAsJsonObject();
                double y=c.getAsJsonArray("origin").get(1).getAsDouble();
                double sy=c.getAsJsonArray("size").get(1).getAsDouble();
                max=Math.max(max,y+sy);
            }
        }
        return max;
    }

    @Test
    void alpha39VerticalCabinetsAreApproximatelyTwoBlocksTall() throws Exception {
        for (String id : List.of("slot","chip_exchange","ticket_exchange")) {
            double h=maxY(id);
            assertTrue(h >= 28.0, id+" should read as a tall arcade cabinet: "+h);
            assertTrue(h <= 32.0, id+" must stay within two blocks: "+h);
        }
    }

    @Test
    void alpha39TablesStayAtFurnitureHeight() throws Exception {
        for (String id : List.of("roulette","poker","blackjack","dice")) {
            assertTrue(maxY(id) <= 16.0, id+" table should remain below one block high");
        }
    }

    @Test
    void alpha39RendererSupportsTallGeometry() throws Exception {
        String renderer=Files.readString(Path.of("src/client/java/com/emipokemon/client/render/CasinoMachineRenderer.java"));
        assertTrue(renderer.contains("rendersOutsideBoundingBox(CasinoMachineBlockEntity"));
        assertTrue(renderer.contains("return true;"));
        assertTrue(renderer.contains("getRenderDistance()"));
    }

    @Test
    void alpha39TexturesContainMaterialVariation() throws Exception {
        for (String id : List.of("slot","chip_exchange","ticket_exchange","roulette","poker","blackjack","dice")) {
            BufferedImage image=ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/block/casino_"+id+".png").toFile());
            assertNotNull(image,id);
            java.util.Set<Integer> colors=new java.util.HashSet<>();
            for (int y=0;y<image.getHeight();y+=4) for (int x=0;x<image.getWidth();x+=4) colors.add(image.getRGB(x,y)&0xFFFFFF);
            assertTrue(colors.size() >= 30,id+" should use patterned material pixels, not flat fills: "+colors.size());
        }
    }

    @Test
    void alpha39VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.39"));
        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java")).contains("0.4.0-alpha.39"));
    }
}
''')
