#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()

def replace_once(path: Path, old: str, new: str):
    text = path.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'alpha75 expected snippet missing in {path}: {old[:140]!r}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

# Version only this alpha.71-derived candidate. Historical source assertions must follow runtime version.
replace_once(root / 'gradle.properties', 'mod_version=0.4.0-alpha.71', 'mod_version=0.4.0-alpha.75')
for path in (root / 'src').rglob('*'):
    if path.is_file() and path.suffix in {'.java', '.json', '.properties'}:
        text = path.read_text(encoding='utf-8')
        if '0.4.0-alpha.71' in text:
            path.write_text(text.replace('0.4.0-alpha.71', '0.4.0-alpha.75'), encoding='utf-8')

# Standard gacha: Eevee is the permanent visual mascot. EMI retains its server-authoritative legendary rotation.
machine = root / 'src/main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java'
replace_once(machine,
'''        if (isEmiThemed()) {\n            selected = Emipokemon.featuredRotation().currentEmiLegendary();\n        } else {\n            // The normal machine stays random: its spotlight is visual only and does not boost roll weights.\n            selected = Emipokemon.featuredRotation().currentStandardSpotlight(banner);\n        }\n''',
'''        if (isEmiThemed()) {\n            // EMI keeps the server-authoritative legendary selected for the current 12-hour rotation.\n            selected = Emipokemon.featuredRotation().currentEmiLegendary();\n        } else {\n            // Alpha.75: Eevee is the permanent mascot above the standard gacha. This remains visual only;\n            // normal gacha roll weights are still fully random and are not boosted by the display.\n            selected = Emipokemon.pokemonCatalog().get("eevee");\n        }\n''')

# Cobblemon 1.7.3 defines CobblemonEntities as a Kotlin object. POKEMON is exposed through
# INSTANCE.getPOKEMON(), not necessarily as a public Java static field. Alpha.71 treated it as a field,
# throwing before a PokemonEntity could be constructed, so the model silently stayed absent.
renderer = root / 'src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java'
replace_once(renderer,
'''        Class<?> entitiesClass = Class.forName("com.cobblemon.mod.common.CobblemonEntities");\n        Object pokemonEntityType = entitiesClass.getField("POKEMON").get(null);\n        Method create = null;\n''',
'''        Class<?> entitiesClass = Class.forName("com.cobblemon.mod.common.CobblemonEntities");\n        Object pokemonEntityType;\n        try {\n            // Cobblemon 1.7.3: Kotlin object -> CobblemonEntities.INSTANCE.getPOKEMON().\n            Object entitiesSingleton = entitiesClass.getField("INSTANCE").get(null);\n            pokemonEntityType = entitiesClass.getMethod("getPOKEMON").invoke(entitiesSingleton);\n        } catch (ReflectiveOperationException kotlinObjectAccessFailure) {\n            // Compatibility fallback for mappings/builds that expose the entity type as a public static field.\n            pokemonEntityType = entitiesClass.getField("POKEMON").get(null);\n        }\n        Method create = null;\n''')

# Keep diagnostics useful if a future Cobblemon API change breaks one particular species.
replace_once(renderer,
'''                Emipokemon.LOGGER.warn("Could not render the featured Cobblemon above a gacha machine", exception);\n''',
'''                Emipokemon.LOGGER.warn("Could not render featured Cobblemon '{}' above gacha machine at {}",\n                        species, machine.getPos(), exception);\n''')

# Alpha.69's historical regression expected the old rotating standard spotlight. Alpha.75 intentionally
# replaces that visual-only behavior with Eevee, so update that one obsolete assertion.
legacy_test = root / 'src/test/java/com/emipokemon/alpha69/Alpha69GachaFixesRegressionTest.java'
replace_once(legacy_test,
             '        assertTrue(machine.contains("currentStandardSpotlight"));\n',
             '        assertTrue(machine.contains("pokemonCatalog().get(\\"eevee\\")"));\n')

# Add focused regression coverage without modifying the alpha.71-approved GUI/text coordinates or assets.
test = root / 'src/test/java/com/emipokemon/alpha75/Alpha75WorldPokemonDisplayRegressionTest.java'
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text(r'''package com.emipokemon.alpha75;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

final class Alpha75WorldPokemonDisplayRegressionTest {
    private static String source(String relative) throws Exception {
        return Files.readString(Path.of("src", relative));
    }

    @Test void standardMachineUsesEeveeOnlyWhileEmiKeepsLegendaryRotation() throws Exception {
        String machine = source("main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java");
        assertTrue(machine.contains("selected = Emipokemon.pokemonCatalog().get(\"eevee\")"));
        assertTrue(machine.contains("selected = Emipokemon.featuredRotation().currentEmiLegendary()"));
        assertFalse(machine.contains("selected = Emipokemon.featuredRotation().currentStandardSpotlight(banner)"));
    }

    @Test void cobblemonKotlinObjectEntityTypeIsResolvedThroughInstanceGetter() throws Exception {
        String renderer = source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java");
        assertTrue(renderer.contains("entitiesClass.getField(\"INSTANCE\").get(null)"));
        assertTrue(renderer.contains("entitiesClass.getMethod(\"getPOKEMON\").invoke(entitiesSingleton)"));
        assertTrue(renderer.contains("entitiesClass.getField(\"POKEMON\").get(null)"));
        assertTrue(renderer.contains("pokemonSpeciesClass.getField(\"INSTANCE\").get(null)"));
        assertTrue(renderer.contains("getEntityRenderDispatcher().render"));
    }

    @Test void approvedAlpha71WorldTextPlacementIsUntouched() throws Exception {
        String renderer = source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java");
        assertTrue(renderer.contains("drawTextLine(matrices, vertices, textRenderer, cameraRotation, 4.18D"));
        assertTrue(renderer.contains("matrices.translate(0.5D, 2.78D + bob, 0.5D)"));
        assertTrue(renderer.contains("Sincronizando…"));
    }

    @Test void approvedGachaArtRemainsByteIdentical() throws Exception {
        assertEquals("694aadcaca9a8dcaace9c26a9e43cf41e056203855cd380670d0b7c208f4d98d",
                sha256(Path.of("src/main/resources/assets/emipokemon/textures/gui/gacha/standard_gacha.png")));
        assertEquals("4c61d6a00e324a82d060b8c7b9a3abe40daa437fda8d8efb5a4eaf45b973e216",
                sha256(Path.of("src/main/resources/assets/emipokemon/textures/gui/gacha/emi_gacha.png")));
        assertEquals("42d060134c7809f2617bcf1aa7a06f02750908eb616fcbbfb3b837dbf98e30cd",
                sha256(Path.of("src/main/resources/assets/emipokemon/textures/block/standard_gacha_machine.png")));
        assertEquals("4d0d76caca51f7d167d3197030d7074e9ac2b7e1ade015c5a01c462ef3e4aea0",
                sha256(Path.of("src/main/resources/assets/emipokemon/textures/block/emi_gacha_machine.png")));
        assertFalse(Files.exists(Path.of("src/main/resources/assets/emipokemon/textures/gui/gacha/reveal_sheet.png")));
    }

    @Test void versionIsAlpha75() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.75"));
        assertTrue(source("main/java/com/emipokemon/Emipokemon.java").contains("0.4.0-alpha.75"));
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
''', encoding='utf-8')

(root / 'CHANGELOG-0.4.0-alpha.75.md').write_text('''# Emipokemon 0.4.0-alpha.75\n\n- Mantiene intactos los retratos y textos validados visualmente en alpha.71.\n- La gacha normal muestra siempre a Eevee como mascota visual.\n- La gacha EMI sigue mostrando el legendario activo de su rotación autoritativa de 12 horas.\n- Corrige la creación del PokemonEntity visual usando la forma real del objeto Kotlin CobblemonEntities de Cobblemon 1.7.3 (`INSTANCE.getPOKEMON()`).\n- Conserva un fallback de compatibilidad y mejora el log de diagnóstico por especie/posición.\n''', encoding='utf-8')
(root / 'GUIA-PRUEBAS-0.4.0-alpha.75.md').write_text('''# Pruebas alpha.75\n\n1. Coloca una gacha normal: el texto debe indicar Eevee y el modelo de Eevee debe aparecer encima.\n2. Coloca una gacha EMI: debe conservar el legendario de EMI vigente y mostrar su modelo encima.\n3. Confirma que los modelos no tapan los textos y que los textos conservan exactamente el aspecto de alpha.71.\n4. Abre ambas gachas y haz x10: los retratos deben seguir funcionando como en alpha.71.\n5. Reinicia cliente/servidor y verifica que el legendario EMI permanezca sincronizado durante la misma rotación.\n''', encoding='utf-8')
print('alpha.75 world Pokemon display fix applied')
