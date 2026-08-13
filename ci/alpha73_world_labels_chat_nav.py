#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()

def replace_once(path: Path, old: str, new: str):
    text = path.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'alpha73 expected snippet missing in {path}: {old[:100]!r}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

# Advance candidate identity everywhere the alpha.72 source artifact intentionally pins it.
gradle = root / 'gradle.properties'
replace_once(gradle, 'mod_version=0.4.0-alpha.72', 'mod_version=0.4.0-alpha.73')
for path in (root / 'src').rglob('*'):
    if not path.is_file() or path.suffix not in {'.java', '.json', '.properties'}:
        continue
    text = path.read_text(encoding='utf-8')
    if '0.4.0-alpha.72' in text:
        path.write_text(text.replace('0.4.0-alpha.72', '0.4.0-alpha.73'), encoding='utf-8')

# Replace the unreliable block-entity text renderer with a real vanilla text_display label.
label_service = root / 'src/main/java/com/emipokemon/gacha/machine/GachaMachineLabelService.java'
label_service.write_text(r'''package com.emipokemon.gacha.machine;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Server-authoritative floating label for each gacha machine.
 *
 * Alpha.73 intentionally uses the same vanilla minecraft:text_display engine already proven by
 * Emipokemon's holograms. The label is a real world entity, so GeckoLib/shader render state cannot
 * hide it and every client receives the exact same title/name without a custom block renderer pass.
 */
final class GachaMachineLabelService {
    private static final String TAG_PREFIX = "emipokemon:gacha_label:";
    private static final double Y_OFFSET = 3.48D;

    private GachaMachineLabelService() { }

    static void reconcile(ServerWorld world, GachaMachineBlockEntity machine) {
        if (world == null || machine == null || machine.isRemoved()) return;
        String tag = tag(machine.getPos());
        List<DisplayEntity.TextDisplayEntity> labels = world.getEntitiesByType(
                EntityType.TEXT_DISPLAY,
                entity -> entity.getCommandTags().contains(tag)
        );

        DisplayEntity.TextDisplayEntity label;
        if (labels.isEmpty()) {
            label = EntityType.TEXT_DISPLAY.create(world);
            if (label == null) return;
            configure(label, world, machine, tag);
            if (!world.spawnEntity(label)) return;
        } else {
            label = labels.getFirst();
            configure(label, world, machine, tag);
            for (int i = 1; i < labels.size(); i++) labels.get(i).discard();
        }
    }

    static void remove(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return;
        String tag = tag(pos);
        for (DisplayEntity.TextDisplayEntity label : world.getEntitiesByType(
                EntityType.TEXT_DISPLAY,
                entity -> entity.getCommandTags().contains(tag))) {
            label.discard();
        }
    }

    private static void configure(DisplayEntity.TextDisplayEntity display, ServerWorld world,
                                  GachaMachineBlockEntity machine, String tag) {
        BlockPos pos = machine.getPos();
        display.refreshPositionAndAngles(
                pos.getX() + 0.5D,
                pos.getY() + Y_OFFSET,
                pos.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        display.setNoGravity(true);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.ignoreCameraFrustum = true;

        String speciesName = machine.getFeaturedPokemonName();
        if (speciesName == null || speciesName.isBlank()) speciesName = "Sincronizando…";
        int titleColor = machine.isEmiThemed() ? 0xFFA6E4 : 0x8DEBFF;
        String title = machine.isEmiThemed() ? "✦ LEGENDARIO DE EMI ✦" : "✦ POKÉMON DE TEMPORADA ✦";

        MutableText text = Text.literal(title).styled(style -> style.withColor(titleColor));
        text.append(Text.literal("\n"));
        text.append(Text.literal(speciesName).styled(style -> style.withColor(0xFFFFFF)));

        NbtCompound nbt = new NbtCompound();
        display.writeNbt(nbt);
        nbt.putString("text", Text.Serialization.toJsonString(text, world.getRegistryManager()));
        nbt.putString("billboard", "center");
        nbt.putFloat("view_range", 6.0F);
        nbt.putFloat("width", 3.0F);
        nbt.putFloat("height", 0.85F);
        nbt.putInt("line_width", 260);
        nbt.putByte("text_opacity", (byte) 0xFF);
        nbt.putByte("shadow", (byte) 1);
        nbt.putByte("see_through", (byte) 1);
        nbt.putByte("default_background", (byte) 0);
        nbt.putInt("background", 0x52000000);

        NbtCompound brightness = new NbtCompound();
        brightness.putInt("block", 15);
        brightness.putInt("sky", 15);
        nbt.put("brightness", brightness);

        display.readNbt(nbt);
        display.refreshPositionAndAngles(
                pos.getX() + 0.5D,
                pos.getY() + Y_OFFSET,
                pos.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        display.ignoreCameraFrustum = true;
        display.addCommandTag(tag);
        display.setCustomName(Text.literal("Emipokemon gacha label"));
        display.setCustomNameVisible(false);
    }

    private static String tag(BlockPos pos) {
        return TAG_PREFIX + pos.asLong();
    }
}
''', encoding='utf-8')

be = root / 'src/main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java'
replace_once(be,
'''    private transient GachaService.PreparedPull preparedPull;\n    private transient GachaMachineState lastAnimatedState;\n''',
'''    private transient GachaService.PreparedPull preparedPull;\n    private transient GachaMachineState lastAnimatedState;\n    private transient boolean seasonalLabelInitialized;\n''')
replace_once(be,
'''            if (staggered % 40L == 0L) {\n                GachaNetworking.syncSeasonalDisplay(serverWorld, machine);\n            }\n            machine.emitSeasonDisplayEffects(serverWorld, staggered);\n''',
'''            if (!machine.seasonalLabelInitialized || staggered % 40L == 0L) {\n                GachaMachineLabelService.reconcile(serverWorld, machine);\n                machine.seasonalLabelInitialized = true;\n                GachaNetworking.syncSeasonalDisplay(serverWorld, machine);\n            }\n            machine.emitSeasonDisplayEffects(serverWorld, staggered);\n''')
replace_once(be,
'''        if (world instanceof ServerWorld serverWorld) {\n            serverWorld.getChunkManager().markForUpdate(pos);\n            GachaNetworking.syncSeasonalDisplay(serverWorld, this);\n        }\n''',
'''        if (world instanceof ServerWorld serverWorld) {\n            serverWorld.getChunkManager().markForUpdate(pos);\n            GachaMachineLabelService.reconcile(serverWorld, this);\n            seasonalLabelInitialized = true;\n            GachaNetworking.syncSeasonalDisplay(serverWorld, this);\n        }\n''')

block = root / 'src/main/java/com/emipokemon/gacha/machine/GachaMachineBlock.java'
replace_once(block,
'''        if (!state.isOf(newState.getBlock()) && !world.isClient) {\n            BlockPos top = pos.up();\n''',
'''        if (!state.isOf(newState.getBlock()) && !world.isClient) {\n            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {\n                GachaMachineLabelService.remove(serverWorld, pos);\n            }\n            BlockPos top = pos.up();\n''')

renderer = root / 'src/client/java/com/emipokemon/client/render/GachaMachineRenderer.java'
replace_once(renderer,
'''        // Text is rendered first with the same proven path as Emipokemon holograms. Keeping it\n        // before GeckoLib prevents the complex machine model from leaking render state into labels.\n        matrices.push();\n        SeasonalPokemonWorldRenderer.drawText(animatable, matrices, buffers, textRenderer);\n        matrices.pop();\n\n''',
'''        // Alpha.73 moved the floating title/name to a server-side vanilla text_display entity.\n        // Do not render another copy from the block-entity renderer.\n''')

button = root / 'src/client/java/com/emipokemon/client/emote/EmotesButtonWidget.java'
text = button.read_text(encoding='utf-8')
text = text.replace('import net.minecraft.client.gui.DrawContext;\n',
                    'import net.minecraft.client.gui.DrawContext;\nimport net.minecraft.client.gui.navigation.GuiNavigation;\nimport net.minecraft.client.gui.navigation.GuiNavigationPath;\n', 1)
text = text.replace('import net.minecraft.text.Text;\n',
                    'import net.minecraft.text.Text;\nimport org.jetbrains.annotations.Nullable;\n', 1)
needle = '''    @Override\n    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {\n        return false;\n    }\n\n'''
if needle not in text:
    raise SystemExit('alpha73 emotes button key guard missing')
replacement = needle + '''    @Override\n    public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {\n        // Mouse-only control: arrow keys in ChatScreen must stay owned by the chat field/history.\n        return null;\n    }\n\n'''
button.write_text(text.replace(needle, replacement, 1), encoding='utf-8')

# Regression guard for both requested fixes.
test = root / 'src/test/java/com/emipokemon/alpha73/Alpha73WorldLabelsAndChatFocusRegressionTest.java'
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text(r'''package com.emipokemon.alpha73;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Alpha73WorldLabelsAndChatFocusRegressionTest {
    private static String source(String relative) throws Exception {
        return Files.readString(Path.of("src", relative));
    }

    @Test
    void gachaLabelsUseVanillaTextDisplayInsteadOfBlockRendererText() throws Exception {
        String service = source("main/java/com/emipokemon/gacha/machine/GachaMachineLabelService.java");
        String renderer = source("client/java/com/emipokemon/client/render/GachaMachineRenderer.java");
        String machine = source("main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java");
        assertTrue(service.contains("EntityType.TEXT_DISPLAY.create(world)"));
        assertTrue(service.contains("nbt.putString(\"billboard\", \"center\")"));
        assertTrue(service.contains("nbt.putByte(\"see_through\", (byte) 1)"));
        assertTrue(service.contains("brightness"));
        assertTrue(machine.contains("GachaMachineLabelService.reconcile(serverWorld, machine)"));
        assertFalse(renderer.contains("SeasonalPokemonWorldRenderer.drawText"));
    }

    @Test
    void emotesButtonIsExcludedFromArrowKeyNavigation() throws Exception {
        String button = source("client/java/com/emipokemon/client/emote/EmotesButtonWidget.java");
        assertTrue(button.contains("getNavigationPath(GuiNavigation navigation)"));
        assertTrue(button.contains("return null;"));
        assertTrue(button.contains("public boolean keyPressed"));
    }

    @Test
    void versionIsAlpha73() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.73"));
        assertTrue(source("main/java/com/emipokemon/Emipokemon.java").contains("0.4.0-alpha.73"));
    }
}
''', encoding='utf-8')

(root / 'CHANGELOG-0.4.0-alpha.73.md').write_text('''# Emipokemon 0.4.0-alpha.73\n\n- Gacha machine floating title/name now uses a real vanilla `minecraft:text_display` entity, the same reliable engine used by Emipokemon holograms.\n- The label is server-authoritative, full-bright, camera-facing and see-through, and is restored automatically if it disappears.\n- Removed the unreliable client block-renderer text pass.\n- The **Emotes** button is now mouse-only for focus navigation, so arrow keys remain available for chat cursor/history instead of selecting the button.\n''', encoding='utf-8')
(root / 'GUIA-PRUEBAS-0.4.0-alpha.73.md').write_text('''# Pruebas alpha.73\n\n1. Coloca una gacha normal y una gacha de Emi. El título y el nombre del Pokémon deben aparecer encima en menos de 2 segundos (normalmente en el primer tick).\n2. Mira las máquinas desde varios ángulos: el texto debe mirar a cámara y seguir visible.\n3. Rompe una máquina: su texto debe desaparecer. Vuelve a colocarla y debe restaurarse.\n4. Abre el chat. Sin abrir el selector de emotes, usa flecha arriba/abajo para historial y izquierda/derecha para el cursor. El botón Emotes no debe recibir selección por flechas.\n5. El botón Emotes debe seguir funcionando normalmente con clic del ratón.\n''', encoding='utf-8')

print('alpha.73 world text_display labels and mouse-only emotes navigation applied')
