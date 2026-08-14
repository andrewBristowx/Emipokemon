from __future__ import annotations
from pathlib import Path
import json, struct, zlib

ROOT = Path('.')

def read(rel): return (ROOT/rel).read_text(encoding='utf-8')
def write(rel, text):
    p=ROOT/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
def replace(rel, old, new):
    t=read(rel)
    if old not in t: raise SystemExit(f'missing expected text in {rel}: {old[:120]}')
    write(rel,t.replace(old,new))

# Version
replace('gradle.properties','mod_version=0.4.0-alpha.78','mod_version=0.4.0-alpha.79')
replace('src/main/java/com/emipokemon/Emipokemon.java','0.4.0-alpha.78','0.4.0-alpha.79')
for p in Path('src/test').rglob('*.java'):
    p.write_text(p.read_text(encoding='utf-8').replace('0.4.0-alpha.78','0.4.0-alpha.79'),encoding='utf-8')

# Gacha display: keep Cobblemon native PokemonItem renderer, lower the anchor and avoid alpha78 over-scaling.
p=Path('src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java')
t=p.read_text(encoding='utf-8')
t=t.replace('matrices.translate(0.5D, 5.02D + bob, 0.5D);','matrices.translate(0.5D, 4.52D + bob, 0.5D);')
t=t.replace('matrices.scale(machine.isEmiThemed() ? 1.7F : 1.5F, machine.isEmiThemed() ? 1.7F : 1.5F, machine.isEmiThemed() ? 1.7F : 1.5F);',
'''float displayScale = machine.isEmiThemed() ? 1.35F : 1.25F;\n            matrices.scale(displayScale, displayScale, displayScale);''')
t=t.replace('* 0.055F;', '* 0.04F;')
p.write_text(t,encoding='utf-8')

# Third-person only: normalize each Geo model to vanilla-like 55-degree handheld pose.
transforms={'sword':(0.44,3.75),'pickaxe':(0.50,3.55),'axe':(0.48,3.60),'shovel':(0.43,3.80),'hoe':(0.48,3.60)}
for n,(scale,ty) in transforms.items():
    p=Path(f'src/main/resources/assets/emipokemon/models/item/emi_{n}.json')
    d=json.loads(p.read_text(encoding='utf-8'))
    d['display']['thirdperson_righthand']={'rotation':[0,-90,55],'translation':[0,ty,0.5],'scale':[scale,scale,scale]}
    d['display']['thirdperson_lefthand']={'rotation':[0,90,-55],'translation':[0,ty,0.5],'scale':[scale,scale,scale]}
    p.write_text(json.dumps(d,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

# Emi armor material: vanilla humanoid geometry, modest tier above netherite.
write('src/main/java/com/emipokemon/armor/EmiArmorMaterial.java','''package com.emipokemon.armor;\n\nimport com.emipokemon.Emipokemon;\nimport net.minecraft.item.ArmorItem;\nimport net.minecraft.item.ArmorMaterial;\nimport net.minecraft.item.Items;\nimport net.minecraft.recipe.Ingredient;\nimport net.minecraft.registry.Registries;\nimport net.minecraft.registry.Registry;\nimport net.minecraft.registry.entry.RegistryEntry;\nimport net.minecraft.sound.SoundEvents;\nimport net.minecraft.util.Identifier;\n\nimport java.util.List;\nimport java.util.Map;\n\n/** Vanilla-shaped Emi armor, intentionally a modest tier above netherite. */\npublic final class EmiArmorMaterial {\n    public static final int DURABILITY_MULTIPLIER = 45;\n    public static final RegistryEntry<ArmorMaterial> INSTANCE = register();\n\n    private EmiArmorMaterial() { }\n\n    private static RegistryEntry<ArmorMaterial> register() {\n        ArmorMaterial material = new ArmorMaterial(\n                Map.of(\n                        ArmorItem.Type.HELMET, 4,\n                        ArmorItem.Type.CHESTPLATE, 9,\n                        ArmorItem.Type.LEGGINGS, 7,\n                        ArmorItem.Type.BOOTS, 4,\n                        ArmorItem.Type.BODY, 14\n                ),\n                20,\n                SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,\n                () -> Ingredient.ofItems(Items.NETHERITE_INGOT),\n                List.of(new ArmorMaterial.Layer(Identifier.of(Emipokemon.MOD_ID, "emi"))),\n                4.0F,\n                0.15F\n        );\n        Registry.register(Registries.ARMOR_MATERIAL, Identifier.of(Emipokemon.MOD_ID, "emi"), material);\n        return Registries.ARMOR_MATERIAL.getEntry(material);\n    }\n}\n''')

p=Path('src/main/java/com/emipokemon/registry/ModRegistries.java')
t=p.read_text(encoding='utf-8')
t=t.replace('import com.emipokemon.Emipokemon;','import com.emipokemon.Emipokemon;\nimport com.emipokemon.armor.EmiArmorMaterial;')
t=t.replace('import net.minecraft.item.BlockItem;','import net.minecraft.item.ArmorItem;\nimport net.minecraft.item.BlockItem;')
anchor='''    public static final Item EMI_HOE = registerItem("emi_hoe", new EmiHoeItem(\n            EmiToolMaterial.INSTANCE,\n            new Item.Settings().attributeModifiers(\n                    MiningToolItem.createAttributeModifiers(EmiToolMaterial.INSTANCE, -3.5F, -1.5F))));\n'''
armor=anchor+'''\n    public static final Item EMI_HELMET = registerItem("emi_helmet", new ArmorItem(\n            EmiArmorMaterial.INSTANCE, ArmorItem.Type.HELMET,\n            new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(EmiArmorMaterial.DURABILITY_MULTIPLIER)).fireproof()));\n    public static final Item EMI_CHESTPLATE = registerItem("emi_chestplate", new ArmorItem(\n            EmiArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE,\n            new Item.Settings().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(EmiArmorMaterial.DURABILITY_MULTIPLIER)).fireproof()));\n    public static final Item EMI_LEGGINGS = registerItem("emi_leggings", new ArmorItem(\n            EmiArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS,\n            new Item.Settings().maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(EmiArmorMaterial.DURABILITY_MULTIPLIER)).fireproof()));\n    public static final Item EMI_BOOTS = registerItem("emi_boots", new ArmorItem(\n            EmiArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS,\n            new Item.Settings().maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(EmiArmorMaterial.DURABILITY_MULTIPLIER)).fireproof()));\n'''
if anchor not in t: raise SystemExit('EMI_HOE registration anchor missing')
t=t.replace(anchor,armor)
t=t.replace('ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(EMI_SWORD));',
'''ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {\n            entries.add(EMI_SWORD);\n            entries.add(EMI_HELMET);\n            entries.add(EMI_CHESTPLATE);\n            entries.add(EMI_LEGGINGS);\n            entries.add(EMI_BOOTS);\n        });''')
t=t.replace('gacha, casino, Emi tools, NPCs, media displays and holograms','gacha, casino, Emi tools/armor, NPCs, media displays and holograms')
p.write_text(t,encoding='utf-8')

# Armor item models and language.
for n in ('helmet','chestplate','leggings','boots'):
    write(f'src/main/resources/assets/emipokemon/models/item/emi_{n}.json',json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'emipokemon:item/emi_{n}'}},indent=2)+'\n')
translations={
'en_us.json':{'emi_helmet':'Emi Helmet','emi_chestplate':'Emi Chestplate','emi_leggings':'Emi Leggings','emi_boots':'Emi Boots'},
'es_es.json':{'emi_helmet':'Casco de Emi','emi_chestplate':'Pechera de Emi','emi_leggings':'Pantalones de Emi','emi_boots':'Botas de Emi'},
'es_mx.json':{'emi_helmet':'Casco de Emi','emi_chestplate':'Pechera de Emi','emi_leggings':'Pantalones de Emi','emi_boots':'Botas de Emi'}}
for fname,vals in translations.items():
    p=Path('src/main/resources/assets/emipokemon/lang')/fname; d=json.loads(p.read_text(encoding='utf-8'))
    for k,v in vals.items(): d[f'item.emipokemon.{k}']=v
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# Vanilla armor/trim tags.
for tag,item in [('head_armor','emi_helmet'),('chest_armor','emi_chestplate'),('leg_armor','emi_leggings'),('foot_armor','emi_boots')]:
    write(f'src/main/resources/data/minecraft/tags/item/{tag}.json',json.dumps({'replace':False,'values':[f'emipokemon:{item}']},indent=2)+'\n')
write('src/main/resources/data/minecraft/tags/item/trimmable_armor.json',json.dumps({'replace':False,'values':['emipokemon:emi_helmet','emipokemon:emi_chestplate','emipokemon:emi_leggings','emipokemon:emi_boots']},indent=2)+'\n')

# Minimal RGBA PNG writer (stdlib only) so CI doesn't need Pillow.
def png(path,w,h,pixels):
    raw=b''.join(b'\x00'+bytes(sum((list(px) for px in row),[])) for row in pixels)
    def chunk(kind,data):
        return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    data=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
    p=Path(path); p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes(data)

D=(24,20,29,255); D2=(43,39,50,255); P=(238,55,143,255); P2=(252,153,200,255); L=(252,213,235,255); G=(241,166,75,255); T=(0,0,0,0)
def blank(w,h,c=T): return [[c for _ in range(w)] for __ in range(h)]
def point(a,x,y,c):
    if 0<=x<len(a[0]) and 0<=y<len(a): a[y][x]=c
def rect(a,x0,y0,x1,y1,c):
    for y in range(y0,y1+1):
        for x in range(x0,x1+1): point(a,x,y,c)
def poly(a,pts,c):
    ys=[p[1] for p in pts]
    for y in range(min(ys),max(ys)+1):
        xs=[]
        for i,(x1,y1) in enumerate(pts):
            x2,y2=pts[(i+1)%len(pts)]
            if y1==y2: continue
            if min(y1,y2)<=y<max(y1,y2): xs.append(x1+(y-y1)*(x2-x1)/(y2-y1))
        xs.sort()
        for i in range(0,len(xs)-1,2):
            for x in range(int(round(xs[i])),int(round(xs[i+1]))+1): point(a,x,y,c)

def armor_icon(kind):
    a=blank(16,16)
    if kind=='helmet':
        poly(a,[(4,2),(11,2),(13,5),(13,11),(11,13),(9,11),(6,11),(4,13),(2,11),(2,5)],D); rect(a,4,3,11,5,P); rect(a,3,6,12,8,P2); rect(a,5,8,10,10,D2); rect(a,6,8,9,9,T); point(a,7,3,G); point(a,8,3,G)
    elif kind=='chestplate':
        poly(a,[(3,2),(6,2),(7,4),(9,4),(10,2),(12,2),(14,5),(12,7),(12,14),(4,14),(4,7),(2,5)],D); poly(a,[(5,3),(7,5),(9,5),(11,3),(12,5),(10,7),(6,7),(4,5)],P2); rect(a,5,8,11,12,P); rect(a,7,8,9,12,D2); rect(a,7,7,9,8,G)
    elif kind=='leggings':
        poly(a,[(4,2),(12,2),(12,8),(10,8),(10,14),(7,14),(7,8),(6,8),(6,14),(3,14),(3,8),(4,8)],D); rect(a,4,3,11,6,P); rect(a,6,3,9,4,G); rect(a,4,7,11,7,P2)
    else:
        poly(a,[(3,6),(7,6),(7,11),(8,12),(8,14),(2,14),(2,11),(3,10)],D); poly(a,[(9,6),(13,6),(13,10),(14,11),(14,14),(8,14),(8,12),(9,11)],D); rect(a,3,7,6,9,P); rect(a,9,7,12,9,P); point(a,5,7,G); point(a,10,7,G)
    return a
for kind in ('helmet','chestplate','leggings','boots'): png(f'src/main/resources/assets/emipokemon/textures/item/emi_{kind}.png',16,16,armor_icon(kind))
for layer in (1,2):
    a=blank(64,32,D)
    for y in range(0,32,8): rect(a,0,y,63,y+1,P); rect(a,0,y+2,63,y+2,D2)
    for x in range(0,64,16): rect(a,x,0,x+1,31,P2)
    for x in (4,12,20,28,36,44,52,60): point(a,x,5,L); point(a,x,21,L)
    for x0,y0,x1,y1 in [(8,8,15,9),(24,8,31,9),(40,8,47,9),(8,24,15,25),(24,24,31,25),(40,24,47,25)]: rect(a,x0,y0,x1,y1,G)
    if layer==2:
        for y in range(32):
            for x in range(64):
                r,g,b,a0=a[y][x]; a[y][x]=(int(r*.92+20*.08),int(g*.92),int(b*.92+12*.08),a0)
    png(f'src/main/resources/assets/emipokemon/textures/models/armor/emi_layer_{layer}.png',64,32,a)

# Historical assertions now target alpha79.
for rel in ['src/test/java/com/emipokemon/alpha75/Alpha75WorldPokemonDisplayRegressionTest.java','src/test/java/com/emipokemon/alpha71/Alpha71VisualSyncRegressionTest.java','src/test/java/com/emipokemon/Alpha77VisualRegressionTest.java']:
    p=Path(rel); s=p.read_text(encoding='utf-8').replace('5.02D + bob','4.52D + bob')
    if p.name=='Alpha77VisualRegressionTest.java': s=s.replace('model.contains("42")','model.contains("55")')
    p.write_text(s,encoding='utf-8')

write('src/test/java/com/emipokemon/alpha79/Alpha79ArmorGachaAndThirdPersonRegressionTest.java',r'''package com.emipokemon.alpha79;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class Alpha79ArmorGachaAndThirdPersonRegressionTest {
 private static String source(String r) throws Exception { return Files.readString(Path.of("src",r)); }
 @Test void versionIsAlpha79() throws Exception { assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.79")); assertTrue(source("main/java/com/emipokemon/Emipokemon.java").contains("0.4.0-alpha.79")); }
 @Test void gachaPlacementAndLabelAreFinalized() throws Exception { String r=source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java"); assertTrue(r.contains("\"GASHA NORMAL\"")); assertFalse(r.contains("POKÉMON DE TEMPORADA")); assertTrue(r.contains("4.52D + bob")); assertTrue(r.contains("1.35F : 1.25F")); }
 @Test void toolsUsePerToolThirdPersonScaleAndKeepFirstPerson() throws Exception { double[] scales={0.44,0.50,0.48,0.43,0.48}; String[] tools={"sword","pickaxe","axe","shovel","hoe"}; for(int i=0;i<tools.length;i++){ String m=Files.readString(Path.of("src/main/resources/assets/emipokemon/models/item/emi_"+tools[i]+".json")); assertTrue(m.contains("55")); assertTrue(m.contains(String.valueOf(scales[i]))); assertTrue(m.contains("0.68")); } }
 @Test void armorStatsAreAboveNetheriteTier() throws Exception { String m=source("main/java/com/emipokemon/armor/EmiArmorMaterial.java"); assertTrue(m.contains("HELMET, 4")); assertTrue(m.contains("CHESTPLATE, 9")); assertTrue(m.contains("LEGGINGS, 7")); assertTrue(m.contains("BOOTS, 4")); assertTrue(m.contains("4.0F")); assertTrue(m.contains("0.15F")); assertTrue(m.contains("DURABILITY_MULTIPLIER = 45")); String r=source("main/java/com/emipokemon/registry/ModRegistries.java"); for(String s:new String[]{"EMI_HELMET","EMI_CHESTPLATE","EMI_LEGGINGS","EMI_BOOTS"}) assertTrue(r.contains(s)); }
 @Test void armorAssetsHaveExpectedDimensions() throws Exception { for(String s:new String[]{"helmet","chestplate","leggings","boots"}){ var i=ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/item/emi_"+s+".png").toFile()); assertEquals(16,i.getWidth()); assertEquals(16,i.getHeight()); } for(int l=1;l<=2;l++){ var i=ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/models/armor/emi_layer_"+l+".png").toFile()); assertEquals(64,i.getWidth()); assertEquals(32,i.getHeight()); } }
 @Test void forcedLegendaryAdminControlsRemain() throws Exception { String c=source("main/java/com/emipokemon/command/GachaCommands.java"), r=source("main/java/com/emipokemon/gacha/banner/FeaturedRotationService.java"); assertTrue(c.contains("literal(\"featured\")")); assertTrue(c.contains("literal(\"force\")")); assertTrue(c.contains("literal(\"clear\")")); assertTrue(r.contains("forceEmiLegendary")); }
}
''')

write('CHANGELOG-0.4.0-alpha.79.md','''# Emipokemon 0.4.0-alpha.79\n\n- Pokémon flotantes: anclaje más bajo (4.52), escala moderada (1.35 Emi / 1.25 normal) y bob más suave.\n- Normal conserva `GASHA NORMAL` sin subtítulo; Eevee sigue como mascota.\n- Herramientas: primera persona intacta; tercera persona usa ángulo vanilla-like 55° y escala específica por herramienta.\n- Nueva armadura Emi: casco/pechera/pantalón/botas, 24 defensa total, toughness 4.0, knockback 0.15, encantabilidad 20, durabilidad x45, fireproof.\n- Armadura usa modelo humanoide vanilla y texturas Emi rosa/negro/dorado.\n- Preservados backend gacha, pity, rates, rotación de 12h, herramientas funcionales y assets protegidos.\n''')
write('GUIA-PRUEBAS-0.4.0-alpha.79.md','''# Guía de pruebas — alpha.79\n\n1. Instala el mismo JAR en servidor y clientes.\n2. Revisa Eevee/legendario: deben quedar más cerca del texto.\n3. Normal: `GASHA NORMAL`, sin nombre debajo. Emi: `LEGENDARIO DE EMI` con nombre.\n4. Prueba `/emipokemon gacha featured emi current`, `force rayquaza` y `clear`.\n5. Revisa las cinco herramientas en F5; primera persona no debe cambiar.\n6. Equipa `emi_helmet`, `emi_chestplate`, `emi_leggings`, `emi_boots` y valida textura/defensa/durabilidad/encantamientos.\n7. Revalida 3x3, tala y azada minera.\n''')
