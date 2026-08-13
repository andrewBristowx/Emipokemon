package com.emipokemon.casino;

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
    void alpha40VerticalCabinetsAreEssentiallyTwoBlocksTall() throws Exception {
        for (String id : List.of("slot","chip_exchange","ticket_exchange")) {
            double h=maxY(id);
            assertTrue(h >= 31.8, id+" should reach essentially two blocks: "+h);
            assertTrue(h <= 32.0, id+" must stay within two blocks: "+h);
        }
    }

    @Test
    void alpha40TablesStayAtFurnitureHeight() throws Exception {
        for (String id : List.of("roulette","poker","blackjack","dice")) {
            assertTrue(maxY(id) <= 16.0, id+" table should remain below one block high");
        }
    }

    @Test
    void alpha40RendererSupportsTallGeometry() throws Exception {
        String renderer=Files.readString(Path.of("src/client/java/com/emipokemon/client/render/CasinoMachineRenderer.java"));
        assertTrue(renderer.contains("rendersOutsideBoundingBox(CasinoMachineBlockEntity"));
        assertTrue(renderer.contains("return true;"));
        assertTrue(renderer.contains("getRenderDistance()"));
    }

    @Test
    void alpha40TexturesContainMaterialVariation() throws Exception {
        for (String id : List.of("slot","chip_exchange","ticket_exchange","roulette","poker","blackjack","dice")) {
            BufferedImage image=ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/block/casino_"+id+".png").toFile());
            assertNotNull(image,id);
            java.util.Set<Integer> colors=new java.util.HashSet<>();
            for (int y=0;y<image.getHeight();y+=4) for (int x=0;x<image.getWidth();x+=4) colors.add(image.getRGB(x,y)&0xFFFFFF);
            assertTrue(colors.size() >= 30,id+" should use patterned material pixels, not flat fills: "+colors.size());
        }
    }

    @Test
    void alpha60VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.64"));
        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java")).contains("0.4.0-alpha.64"));
    }
}
