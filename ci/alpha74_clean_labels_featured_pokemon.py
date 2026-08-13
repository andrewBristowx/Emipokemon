#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()

def replace_once(path: Path, old: str, new: str):
    text = path.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'alpha74 expected snippet missing in {path}: {old[:100]!r}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

replace_once(root / 'gradle.properties', 'mod_version=0.4.0-alpha.73', 'mod_version=0.4.0-alpha.74')
for path in (root / 'src').rglob('*'):
    if path.is_file() and path.suffix in {'.java', '.json', '.properties'}:
        text = path.read_text(encoding='utf-8')
        if '0.4.0-alpha.73' in text:
            path.write_text(text.replace('0.4.0-alpha.73', '0.4.0-alpha.74'), encoding='utf-8')

label = root / 'src/main/java/com/emipokemon/gacha/machine/GachaMachineLabelService.java'
text = label.read_text(encoding='utf-8')
text = text.replace('import net.minecraft.util.math.BlockPos;\n', 'import net.minecraft.util.math.BlockPos;\nimport net.minecraft.util.math.Direction;\n', 1)
text = text.replace('''        BlockPos pos = machine.getPos();\n        display.refreshPositionAndAngles(\n                pos.getX() + 0.5D,\n                pos.getY() + Y_OFFSET,\n                pos.getZ() + 0.5D,\n                0.0F,\n                0.0F\n        );\n''','''        BlockPos pos = machine.getPos();\n        Direction facing = machine.getCachedState().contains(GachaMachineBlock.FACING)\n                ? machine.getCachedState().get(GachaMachineBlock.FACING)\n                : Direction.NORTH;\n        float fixedYaw = facing.asRotation();\n        double labelX = pos.getX() + 0.5D + facing.getOffsetX() * 0.10D;\n        double labelZ = pos.getZ() + 0.5D + facing.getOffsetZ() * 0.10D;\n        display.refreshPositionAndAngles(labelX, pos.getY() + Y_OFFSET, labelZ, fixedYaw, 0.0F);\n''', 1)
text = text.replace('nbt.putString("billboard", "center");', 'nbt.putString("billboard", "fixed");', 1)
text = text.replace('nbt.putByte("shadow", (byte) 1);', 'nbt.putByte("shadow", (byte) 0);', 1)
text = text.replace('nbt.putInt("background", 0x52000000);', 'nbt.putInt("background", 0x00000000);', 1)
text = text.replace('''        display.refreshPositionAndAngles(\n                pos.getX() + 0.5D,\n                pos.getY() + Y_OFFSET,\n                pos.getZ() + 0.5D,\n                0.0F,\n                0.0F\n        );\n''','''        display.refreshPositionAndAngles(labelX, pos.getY() + Y_OFFSET, labelZ, fixedYaw, 0.0F);\n''', 1)
label.write_text(text, encoding='utf-8')

vanilla = root / 'src/main/java/com/emipokemon/hologram/VanillaTextHologram.java'
text = vanilla.read_text(encoding='utf-8')
text = text.replace('nbt.putByte("shadow", (byte) 1);', 'nbt.putByte("shadow", (byte) 0);', 1)
text = text.replace('nbt.putInt("background", 0x40000000);', 'nbt.putInt("background", 0x00000000);', 1)
vanilla.write_text(text, encoding='utf-8')

legacy = root / 'src/client/java/com/emipokemon/client/render/HologramRenderer.java'
replace_once(legacy, '''        // SEE_THROUGH uses the shader-safe text layer. The four black passes form an opaque\n        // outline, and the final normal pass keeps vanilla depth behaviour when shaders allow it.\n        int outline = 0xFF130A16;\n        for (int[] offset : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {\n            textRenderer.draw(text, x + offset[0], offset[1], outline, false,\n                    matrix, vertices, TextRenderer.TextLayerType.SEE_THROUGH, 0,\n                    LightmapTextureManager.MAX_LIGHT_COORDINATE);\n        }\n        textRenderer.draw(text, x, 0.0F, entity.hologramColor(), false,\n                matrix, vertices, TextRenderer.TextLayerType.SEE_THROUGH, 0xB0000000,\n                LightmapTextureManager.MAX_LIGHT_COORDINATE);\n        textRenderer.draw(text, x, 0.0F, entity.hologramColor(), false,\n                matrix, vertices, TextRenderer.TextLayerType.NORMAL, 0,\n                LightmapTextureManager.MAX_LIGHT_COORDINATE);\n''', '''        // Alpha.74: no black rectangle, fake outline or drop shadow on floating text.\n        textRenderer.draw(text, x, 0.0F, entity.hologramColor(), false,\n                matrix, vertices, TextRenderer.TextLayerType.SEE_THROUGH, 0,\n                LightmapTextureManager.MAX_LIGHT_COORDINATE);\n''')

seasonal = root / 'src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java'
text = seasonal.read_text(encoding='utf-8')
text = text.replace('import net.minecraft.util.math.RotationAxis;\n', 'import net.minecraft.util.math.RotationAxis;\nimport net.minecraft.util.math.Direction;\n', 1)
text = text.replace('''            float fit = Math.min(0.82F, Math.min(1.45F / width, 1.35F / height));\n            fit = Math.max(0.28F, fit);\n\n            matrices.push();\n            matrices.translate(0.5D, 2.78D + bob, 0.5D);\n            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - client.gameRenderer.getCamera().getYaw()));\n''','''            float fit = Math.min(0.72F, Math.min(1.25F / width, 1.05F / height));\n            fit = Math.max(0.22F, fit);\n            Direction facing = machine.getCachedState().contains(com.emipokemon.gacha.machine.GachaMachineBlock.FACING)\n                    ? machine.getCachedState().get(com.emipokemon.gacha.machine.GachaMachineBlock.FACING)\n                    : Direction.NORTH;\n\n            matrices.push();\n            matrices.translate(0.5D, 4.18D + bob, 0.5D);\n            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing.asRotation()));\n''', 1)
seasonal.write_text(text, encoding='utf-8')

test = root / 'src/test/java/com/emipokemon/alpha74/Alpha74CleanLabelsAndFeaturedPokemonRegressionTest.java'
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text(r'''package com.emipokemon.alpha74;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Alpha74CleanLabelsAndFeaturedPokemonRegressionTest {
    private static String source(String relative) throws Exception { return Files.readString(Path.of("src", relative)); }

    @Test void gachaLabelsAreFixedAndClean() throws Exception {
        String labels = source("main/java/com/emipokemon/gacha/machine/GachaMachineLabelService.java");
        assertTrue(labels.contains("nbt.putString(\"billboard\", \"fixed\")"));
        assertTrue(labels.contains("nbt.putByte(\"shadow\", (byte) 0)"));
        assertTrue(labels.contains("nbt.putInt(\"background\", 0x00000000)"));
        assertTrue(labels.contains("facing.asRotation()"));
        assertFalse(labels.contains("nbt.putString(\"billboard\", \"center\")"));
    }

    @Test void floatingHologramsHaveNoBlackBackdrop() throws Exception {
        String vanilla = source("main/java/com/emipokemon/hologram/VanillaTextHologram.java");
        String legacy = source("client/java/com/emipokemon/client/render/HologramRenderer.java");
        assertTrue(vanilla.contains("nbt.putByte(\"shadow\", (byte) 0)"));
        assertTrue(vanilla.contains("nbt.putInt(\"background\", 0x00000000)"));
        assertFalse(legacy.contains("0xB0000000"));
        assertFalse(legacy.contains("int outline ="));
    }

    @Test void featuredPokemonIsAboveLabelAndNotCameraTracked() throws Exception {
        String renderer = source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java");
        assertTrue(renderer.contains("matrices.translate(0.5D, 4.18D + bob, 0.5D)"));
        assertTrue(renderer.contains("facing.asRotation()"));
        assertFalse(renderer.contains("client.gameRenderer.getCamera().getYaw()"));
        assertTrue(renderer.contains("Math.min(0.72F"));
    }

    @Test void versionIsAlpha74() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.74"));
        assertTrue(source("main/java/com/emipokemon/Emipokemon.java").contains("0.4.0-alpha.74"));
    }
}
''', encoding='utf-8')

(root / 'CHANGELOG-0.4.0-alpha.74.md').write_text('''# Emipokemon 0.4.0-alpha.74\n\n- Elimina fondo y sombra negra de todos los textos flotantes de Emipokemon.\n- Los carteles de gacha dejan de seguir la cámara y quedan fijados al frente físico de cada máquina.\n- El Pokémon destacado se renderiza por encima del cartel y reduce su escala máxima para no tapar ninguna línea.\n- El Pokémon destacado queda orientado con el frente de la máquina.\n- Conserva la corrección del botón Emotes de alpha.73.\n''', encoding='utf-8')
(root / 'GUIA-PRUEBAS-0.4.0-alpha.74.md').write_text('''# Pruebas alpha.74\n\n1. Ambas gashas: texto sin rectángulo/sombra negra.\n2. Muévete lateralmente: el texto no debe seguir la cámara.\n3. El Pokémon debe aparecer por encima del texto sin cubrir ninguna línea.\n4. Prueba máquinas mirando norte/sur/este/oeste.\n5. Otros hologramas de Emipokemon tampoco deben tener fondo/sombra negra.\n6. Las flechas del chat deben seguir sin seleccionar Emotes.\n''', encoding='utf-8')
print('alpha.74 applied')
