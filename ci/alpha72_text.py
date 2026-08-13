#!/usr/bin/env python3
from pathlib import Path
ROOT = Path.cwd()
def read(rel): return (ROOT / rel).read_text(encoding="utf-8")
def write(rel, s): (ROOT / rel).write_text(s, encoding="utf-8")

p = "gradle.properties"
s = read(p)
if "mod_version=0.4.0-alpha.71" not in s: raise SystemExit("Expected alpha.71 source version")
write(p, s.replace("mod_version=0.4.0-alpha.71", "mod_version=0.4.0-alpha.72", 1))

p = "src/client/java/com/emipokemon/client/render/GachaMachineRenderer.java"
s = read(p)
start = s.index("    @Override\n    public void render(")
end = s.index("    @Override\n    public @Nullable RenderLayer getRenderType", start)
write(p, s[:start] + '    @Override\n    public void render(GachaMachineBlockEntity animatable, float partialTick, MatrixStack matrices,\n                       VertexConsumerProvider buffers, int packedLight, int packedOverlay) {\n        // Text is rendered first with the same proven path as Emipokemon holograms. Keeping it\n        // before GeckoLib prevents the complex machine model from leaking render state into labels.\n        matrices.push();\n        SeasonalPokemonWorldRenderer.drawText(animatable, matrices, buffers, textRenderer);\n        matrices.pop();\n\n        matrices.push();\n        super.render(animatable, partialTick, matrices, buffers, packedLight, packedOverlay);\n        matrices.pop();\n\n        // Keep the experimental 3D Pokémon separate so text visibility can be validated independently.\n        matrices.push();\n        SeasonalPokemonWorldRenderer.drawPokemon(animatable, partialTick, matrices, buffers);\n        matrices.pop();\n    }\n\n' + s[end:])

p = "src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java"
s = read(p).replace("import org.joml.Quaternionf;\n", "", 1)
start = s.index("    static void draw(")
end = s.index("    private static void renderWorldPokemon", start)
s = s[:start] + '    static void drawText(GachaMachineBlockEntity machine, MatrixStack matrices,\n                         VertexConsumerProvider vertices, TextRenderer textRenderer) {\n        if (machine.getWorld() == null) return;\n        String species = normalizeSpecies(machine.getFeaturedSpeciesId());\n        String displayName = machine.getFeaturedPokemonName();\n        if ((displayName == null || displayName.isBlank()) && !species.isBlank()) {\n            displayName = readableName(species);\n        }\n        String title = machine.isEmiThemed() ? "✦ LEGENDARIO DE EMI ✦" : "✦ POKÉMON DE TEMPORADA ✦";\n        int titleColor = machine.isEmiThemed() ? 0xFFFFA6E4 : 0xFF8DEBFF;\n\n        // Alpha.72 deliberately renders this BEFORE GeckoLib\'s machine model. It uses the exact\n        // billboard/outline path already proven by HologramRenderer, so model render state cannot\n        // swallow the label. The text is independent from the experimental Pokémon renderer.\n        drawTextLine(matrices, vertices, textRenderer, 3.72D, title, titleColor, 0.0215F);\n        drawTextLine(matrices, vertices, textRenderer, 3.43D,\n                displayName == null || displayName.isBlank() ? "Sincronizando…" : displayName,\n                0xFFFFFFFF, 0.0275F);\n    }\n\n    static void drawPokemon(GachaMachineBlockEntity machine, float partialTick, MatrixStack matrices,\n                            VertexConsumerProvider vertices) {\n        if (machine.getWorld() == null) return;\n        String species = normalizeSpecies(machine.getFeaturedSpeciesId());\n        if (species.isBlank()) return;\n        MinecraftClient client = MinecraftClient.getInstance();\n        float bob = (float) Math.sin(machine.getWorld().getTime() * 0.075D\n                + machine.getPos().asLong() * 0.01D) * 0.055F;\n        renderWorldPokemon(machine, species, partialTick, matrices, vertices, client, bob);\n    }\n\n' + s[end:]
start = s.index("    private static void drawTextLine(")
end = s.index("    private static String normalizeSpecies", start)
write(p, s[:start] + '    private static void drawTextLine(MatrixStack matrices, VertexConsumerProvider vertices, TextRenderer renderer,\n                                     double y, String value, int color, float scale) {\n        if (value == null || value.isBlank()) return;\n        MinecraftClient client = MinecraftClient.getInstance();\n        matrices.push();\n        matrices.translate(0.5D, y, 0.5D);\n        matrices.multiply(client.getEntityRenderDispatcher().getRotation());\n        matrices.scale(-scale, -scale, scale);\n        Matrix4f matrix = matrices.peek().getPositionMatrix();\n        Text text = Text.literal(value);\n        float x = -renderer.getWidth(text) / 2.0F;\n\n        int outline = 0xFF130A16;\n        for (int[] offset : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {\n            renderer.draw(text, x + offset[0], offset[1], outline, false, matrix, vertices,\n                    TextRenderer.TextLayerType.SEE_THROUGH, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);\n        }\n        renderer.draw(text, x, 0.0F, color, false, matrix, vertices,\n                TextRenderer.TextLayerType.SEE_THROUGH, 0xD0000000,\n                LightmapTextureManager.MAX_LIGHT_COORDINATE);\n        renderer.draw(text, x, 0.0F, color, false, matrix, vertices,\n                TextRenderer.TextLayerType.NORMAL, 0,\n                LightmapTextureManager.MAX_LIGHT_COORDINATE);\n        matrices.pop();\n    }\n\n' + s[end:])

p = "src/test/java/com/emipokemon/alpha71/Alpha71VisualSyncRegressionTest.java"
s = read(p)
start = s.index("    @Test\n    void seasonalLabelIsAbovePhysicalCabinet")
end = s.index("    @Test", start + 10)
write(p, s[:start] + '    @Test\n    void seasonalLabelIsAbovePhysicalCabinet() throws Exception {\n        String renderer = source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java");\n        assertTrue(renderer.contains("3.72D"));\n        assertTrue(renderer.contains("3.43D"));\n        assertTrue(renderer.contains("2.78D + bob"));\n        assertTrue(renderer.contains("Sincronizando…"));\n        assertTrue(renderer.contains("drawText("));\n        assertTrue(renderer.contains("drawPokemon("));\n    }\n\n' + s[end:])

(ROOT / "CHANGELOG-0.4.0-alpha.72.md").write_text("""# Emipokemon 0.4.0-alpha.72

## Objetivo
Prueba visual aislada para hacer visible y legible el texto del Pokemon destacado sobre las maquinas gacha.

## Cambios
- El texto se renderiza antes del modelo GeckoLib para evitar que el estado del modelo lo oculte.
- Se separa el render del texto del render experimental del Pokemon 3D.
- Cartel estandar: `POKÉMON DE TEMPORADA`; cartel Emi: `LEGENDARIO DE EMI`.
- Titulo en Y=3.72 y nombre en Y=3.43, ambos como billboard hacia la camara.
- Texto `SEE_THROUGH`, luz maxima, fondo oscuro y contorno para mejorar contraste.
- Esta build no cambia probabilidades, pity, tickets ni las reglas del gacha.

Base: 0.4.0-alpha.71 validada.
""", encoding="utf-8")
(ROOT / "GUIA-PRUEBAS-0.4.0-alpha.72.md").write_text("""# Prueba alpha.72

1. Sustituye alpha.71 por `emipokemon-0.4.0-alpha.72.jar` en cliente y servidor.
2. Reinicia por completo.
3. Coloca una maquina estandar y una maquina de Emi.
4. Miralas de frente a una distancia parecida a la captura de referencia.
5. Confirma si aparece el titulo y el nombre del Pokemon encima de cada maquina.
6. Envia una captura para ajustar altura/tamano si hace falta.

No validar aun eventos especiales: esta build aisla la correccion visual del texto.
""", encoding="utf-8")

checks = {
 "gradle.properties":"mod_version=0.4.0-alpha.72",
 "src/client/java/com/emipokemon/client/render/GachaMachineRenderer.java":"SeasonalPokemonWorldRenderer.drawText",
 "src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java":"3.72D",
 "src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java":"TextRenderer.TextLayerType.SEE_THROUGH",
}
for rel, needle in checks.items():
    if needle not in read(rel): raise SystemExit(f"alpha.72 verification failed: {rel} missing {needle}")
print("alpha.72 text overlay applied")
