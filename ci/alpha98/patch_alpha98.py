#!/usr/bin/env python3
from pathlib import Path
import base64, hashlib, re, struct

ROOT=Path.cwd()
def read(p): return (ROOT/p).read_text(encoding="utf-8")
def write(p,s):
    q=ROOT/p; q.parent.mkdir(parents=True,exist_ok=True); q.write_text(s,encoding="utf-8")
def require(x,msg):
    if not x: raise SystemExit(msg)

require("mod_version=0.4.0-alpha.90" in read("gradle.properties"), "requires alpha.90 base")
require('VERSION = "0.4.0-alpha.90"' in read("src/main/java/com/emipokemon/Emipokemon.java"), "alpha.90 runtime marker missing")
require("registerEmiToolRenderer(ModRegistries.EMI_SWORD" in read("src/client/java/com/emipokemon/client/EmipokemonClient.java"), "alpha.90 tool renderer missing")

write("gradle.properties", read("gradle.properties").replace("mod_version=0.4.0-alpha.90","mod_version=0.4.0-alpha.98"))
write("src/main/java/com/emipokemon/Emipokemon.java", read("src/main/java/com/emipokemon/Emipokemon.java").replace('VERSION = "0.4.0-alpha.90"','VERSION = "0.4.0-alpha.98"'))

# Remove only the custom GeckoLib path for tools; armor GeckoLib code remains.
p="src/client/java/com/emipokemon/client/EmipokemonClient.java"; s=read(p)
for imp in ("import com.emipokemon.client.render.EmiToolGeoRenderer;\n","import com.emipokemon.tools.EmiGeoToolItem;\n","import net.minecraft.client.render.item.BuiltinModelItemRenderer;\n"):
    s=s.replace(imp,"")
s=re.sub(r'^\s*registerEmiToolRenderer\(ModRegistries\.EMI_(?:SWORD|PICKAXE|AXE|SHOVEL|HOE),\s*"[^"]+"\);\n','',s,flags=re.M)
s=re.sub(r'\n\s*private static void registerEmiToolRenderer\(net\.minecraft\.item\.Item item, String modelId\) \{.*?\n\s*\}\n\s*private static void registerEmiArmorRenderer', '\n\n    private static void registerEmiArmorRenderer', s, flags=re.S)
require("registerEmiToolRenderer" not in s,"tool renderer registration survived")
require("registerEmiArmorRenderer(ModRegistries.EMI_HELMET" in s,"armor renderer was removed")
write(p,s)

bases={"EmiSwordItem.java":"SwordItem","EmiPickaxeItem.java":"PickaxeItem","EmiAxeItem.java":"AxeItem","EmiShovelItem.java":"ShovelItem","EmiHoeItem.java":"HoeItem"}
for fn,base in bases.items():
    p="src/main/java/com/emipokemon/tools/"+fn; s=read(p)
    s=re.sub(r'^import org\.apache\.commons\.lang3\.mutable\.MutableObject;\n','',s,flags=re.M)
    s=re.sub(r'^import software\.bernie\.geckolib\.[^;]+;\n','',s,flags=re.M)
    s=s.replace(f"extends {base} implements EmiGeoToolItem",f"extends {base}")
    s=re.sub(r'^\s*private final AnimatableInstanceCache geoCache = GeckoLibUtil\.createInstanceCache\(this\);\n','',s,flags=re.M)
    s=re.sub(r'^\s*public final MutableObject<GeoRenderProvider> renderProviderHolder = new MutableObject<>\(\);\n','',s,flags=re.M)
    s=re.sub(r'^\s*SingletonGeoAnimatable\.registerSyncedAnimatable\(this\);\n','',s,flags=re.M)
    s=re.sub(r'^\s*@Override public (?:String emiGeoModelId\(\)|AnimatableInstanceCache emiGeoCache\(\)|MutableObject<GeoRenderProvider> emiRenderProviderHolder\(\)) \{[^\n]+\}\n','',s,flags=re.M)
    require("implements EmiGeoToolItem" not in s and "SingletonGeoAnimatable" not in s, fn+" still uses Gecko tool renderer")
    write(p,s)

textures={'sword': 'iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA4klEQVR4nJ2QIQvCUBSF78QkIgZZEtNYGAZZFHlpQcRs9O+Y3494/gCTyfSSsPTAZLgYFQ1zZemY3pCBc2+nXG74zjn3etRCWki04UoYOYAcCPzIzcjCGRfuBhY2itvBGRcwipFx4Q7z+QWjGHx+/Ya1kKh+VwsJoxireIPTLq1PtknWxAmu3qqFxGmXusHfBhkX7nD1FKO4Edz5XuZhgmDUp2S9oMftSeF4So0bzMME+XFZpgZ+hMCPsIo3tU269uuHyZtm2z1d7xePiMpJRPUNtJB/U+rkjfpjDHsDqqQ21gcZGschk6IwoQAAAABJRU5ErkJggg==', 'pickaxe': 'iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAuElEQVR4nGNgGGjAiEviiN3E/+hiNofycarH0Pz/y384/vTg5/8Hp979P2I38f8Ru4n/rdRcMAzHaoiKmBYc72s/+//BqXf/v+zw+K8ipkXYAHRgpeYCN4RkA6zUXP6/CJqL4hKiDbFSc0FxtoqY1v9PD34SZwCyzTAxFTGt//+//CdsALrNuAxgwqXZxsyBwSDuEcOdV9eIi3tkzRMLluJ1Ik45KzWX/2UxLeTHM9mJBOYssjVTAgCC5Is1IY0vogAAAABJRU5ErkJggg==', 'axe': 'iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAy0lEQVR4nGNgoAc4Yjfx/xG7if/J1vz/y////7/8x2oICyHN1tvyGPZPPsfAwMDAwIZFDRM+zXqLMuCaGRgYGGR6YhleBM1FcQVOAxJuzEThKzkrMtzbe5/B5kgvPkejAhUxrf8qYlr/H5x6939f+9n/XkZh//e1n/2vIqYFdwVOFzAwMDDceXWNUUxAioGjYz1DWn8sQ0loOfG2MzAwMFipufz/ssMDbiPMRURrfhE0l3gN6JonFiwlX3NZTMsA2YwcYCQDkkKYmgAAWYZm22K+bVkAAAAASUVORK5CYII=', 'shovel': 'iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAk0lEQVR4nGNgGGjASIriI3YT/8PYNofySdLLcMRu4v//X/7DsYqY1n/CutA0Pzj17v+DU+/+f3rwE24AEzGa9RZlMDy89p7h3t77JLkaDqzUXP7vaz+LYTvRmr/s8PivIqZFmWYGBgYGkjW/CJpLmo3ImusiWuis2UrN5b+Vmsv/iQVLybcZOcDIAipiWpQZQA4AACzccC3nCXbqAAAAAElFTkSuQmCC', 'hoe': 'iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAkUlEQVR4nGNgoBAw4pPcbtX+H8b2PFaJVy1Wzf+//IdjZMOIBhJCcv8lhOT+f3rw8///L///exmF/bdSc/n/ImguaYZJCMn9f3Dq3f9PD37+/7LD47+EkBzprrFSc/n/4NS7//vaz5JuAMzZMC+RrJkiZ8NsJkszRTZPLFhKvuaymJYBsplsPzMwIJItWZopAQAjCG0TK+aQxwAAAABJRU5ErkJggg=='}
for tool,b64 in textures.items():
    p=ROOT/f"src/main/resources/assets/emipokemon/textures/item/emi_{tool}.png"; p.write_bytes(base64.b64decode(b64))
    data=p.read_bytes(); require(data[:8]==b"\x89PNG\r\n\x1a\n",tool+" not png")
    require(struct.unpack(">II",data[16:24])==(16,16),tool+" not 16x16")
    write(f"src/main/resources/assets/emipokemon/models/item/emi_{tool}.json", '{\n  "parent": "minecraft:item/handheld",\n  "textures": {\n    "layer0": "emipokemon:item/emi_'+tool+'"\n  }\n}\n')

# Remove the historical test that intentionally enforced the discarded alpha.76 GeoItem approach.
old=ROOT/"src/test/java/com/emipokemon/alpha76/Alpha76GeoToolsRegressionTest.java"
if old.exists(): old.unlink()

write("src/test/java/com/emipokemon/alpha98/Alpha98VanillaHandheldToolsRegressionTest.java", 'package com.emipokemon.alpha98;\n\nimport org.junit.jupiter.api.Test;\n\nimport javax.imageio.ImageIO;\nimport java.nio.file.Files;\nimport java.nio.file.Path;\nimport java.security.MessageDigest;\nimport java.util.HexFormat;\nimport java.util.Map;\n\nimport static org.junit.jupiter.api.Assertions.*;\n\nfinal class Alpha98VanillaHandheldToolsRegressionTest {\n    private static final Map<String, String> VANILLA_1211_MASK_SHA256 = Map.ofEntries(\n            Map.entry("sword", "d3250ae79059069e09ef003438ef9d46b086addbc676193690aea327be245240"),\n            Map.entry("pickaxe", "59513be2ea3913d3afa289c049bfe472629961118c2296ae5f684e580d3a400b"),\n            Map.entry("axe", "10fb5ca663837caa183d72ce4d2dbfae6b88356d640c9a4ca1f44980c039bc81"),\n            Map.entry("shovel", "f22732fc3b17f385b6ee8dc02de9a9a6dc9d060a9d1e2048f5b0bbeb51ffc895"),\n            Map.entry("hoe", "c8be7f8771d94296af2b6a989e8cb5c87236cee44b2ab1659182b86c92f4c411")\n    );\n\n    @Test void everyEmiToolKeepsTheExactNetherite1211OpaquePixelMask() throws Exception {\n        for (var entry : VANILLA_1211_MASK_SHA256.entrySet()) {\n            String tool = entry.getKey();\n            var image = ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/item/emi_" + tool + ".png").toFile());\n            assertNotNull(image, tool);\n            assertEquals(16, image.getWidth(), tool);\n            assertEquals(16, image.getHeight(), tool);\n            byte[] mask = new byte[256];\n            int i = 0;\n            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {\n                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;\n                mask[i++] = (byte) (alpha == 0 ? 0 : 255);\n            }\n            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mask));\n            assertEquals(entry.getValue(), digest, tool);\n        }\n    }\n\n    @Test void vanillaHandheldParentOwnsAllDisplayTransforms() throws Exception {\n        for (String tool : VANILLA_1211_MASK_SHA256.keySet()) {\n            String model = Files.readString(Path.of("src/main/resources/assets/emipokemon/models/item/emi_" + tool + ".json"));\n            assertTrue(model.contains("minecraft:item/handheld"), tool);\n            assertFalse(model.contains("display"), tool);\n            assertFalse(model.contains("scale"), tool);\n            assertFalse(model.contains("translation"), tool);\n            assertFalse(model.contains("rotation"), tool);\n        }\n    }\n\n    @Test void gameplayToolLogicIsPreserved() throws Exception {\n        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/tools/EmiPickaxeItem.java")).contains("minePlane"));\n        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/tools/EmiShovelItem.java")).contains("minePlane"));\n        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/tools/EmiAxeItem.java")).contains("fellTree"));\n        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/tools/EmiToolActions.java")).contains("MAX_TREE_LOGS = 192"));\n        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/tools/EmiHoeItem.java")).contains("0.16"));\n    }\n\n    @Test void onlyToolGeoRegistrationWasRemovedArmorGeoRegistrationRemains() throws Exception {\n        String client = Files.readString(Path.of("src/client/java/com/emipokemon/client/EmipokemonClient.java"));\n        assertFalse(client.contains("registerEmiToolRenderer"));\n        assertTrue(client.contains("registerEmiArmorRenderer(ModRegistries.EMI_HELMET)"));\n        assertTrue(client.contains("registerEmiArmorRenderer(ModRegistries.EMI_CHESTPLATE)"));\n        assertTrue(client.contains("registerEmiArmorRenderer(ModRegistries.EMI_LEGGINGS)"));\n        assertTrue(client.contains("registerEmiArmorRenderer(ModRegistries.EMI_BOOTS)"));\n    }\n}\n')
write("CHANGELOG-0.4.0-alpha.98.md", '# Emipokemon 0.4.0-alpha.98\n\n## Herramientas Emi: render vanilla exacto\n\n- Vuelve las cinco herramientas Emi al pipeline vanilla `minecraft:item/handheld`.\n- Retira exclusivamente el registro GeckoLib de espada, pico, hacha, pala y azada; la armadura Emi conserva GeckoLib sin cambios.\n- Sustituye los sprites anteriores de 32x32/transparencias residuales por sprites 16x16 basados píxel a píxel en la máscara de las herramientas de netherite de Minecraft 1.21.1.\n- Conserva exactamente silueta, canvas, píxeles transparentes y transforms heredados de vanilla; solo cambia la paleta a rosa/lila/negro/lavanda con pequeños acentos dorados.\n- Mantiene intactas las funciones validadas: espada superior a netherite, pico 3x3, hacha taladora, pala 3x3 y probabilidad de minerales al arar.\n- Añade pruebas de regresión para máscara vanilla, ausencia de transforms manuales y conservación del renderer GeckoLib de la armadura.\n')
write("GUIA-PRUEBAS-0.4.0-alpha.98.md", '# Guía de pruebas — Emipokemon 0.4.0-alpha.98\n\n1. Comparar cada herramienta Emi con su equivalente de netherite en inventario, primera persona, tercera persona, suelo y marco. Deben ocupar la misma silueta/posición; solo cambia el color.\n2. Confirmar que la armadura Emi conserva su render GeckoLib, casco abierto y corazones.\n3. Pico: romper un bloque minable y verificar minería 3x3.\n4. Hacha: romper un tronco de árbol y verificar tala.\n5. Pala: romper terreno compatible y verificar 3x3.\n6. Azada: arar repetidamente y verificar que mantiene la probabilidad de recompensa mineral.\n7. Espada: verificar atributos superiores a netherite y ataque normal.\n')

for tool in textures:
    model=read(f"src/main/resources/assets/emipokemon/models/item/emi_{tool}.json")
    require('"parent": "minecraft:item/handheld"' in model and '"display"' not in model,tool+" model invalid")
require("mod_version=0.4.0-alpha.98" in read("gradle.properties"),"version bump failed")
print("alpha.98 patch applied")
