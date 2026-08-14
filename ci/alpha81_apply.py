#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel): return (ROOT/rel).read_text()
def write(rel, text):
    p=ROOT/rel; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(text)
def replace(rel, old, new):
    s=read(rel)
    if old not in s: raise SystemExit(f'missing expected text in {rel}: {old[:100]!r}')
    write(rel, s.replace(old,new))

for rel in ['gradle.properties','src/main/java/com/emipokemon/Emipokemon.java']:
    replace(rel,'0.4.0-alpha.80','0.4.0-alpha.81')
for p in (ROOT/'src/test/java').rglob('*.java'):
    p.write_text(p.read_text().replace('0.4.0-alpha.80','0.4.0-alpha.81'))

write('src/main/java/com/emipokemon/gacha/machine/GachaMachineTextDisplayService.java', r'''package com.emipokemon.gacha.machine;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.ArrayList;
import java.util.List;
final class GachaMachineTextDisplayService {
    private static final String TAG_PREFIX = "emipokemon:gacha-label:";
    private static final double LABEL_Y = 3.92D;
    private GachaMachineTextDisplayService() {}
    static void reconcile(ServerWorld world, GachaMachineBlockEntity machine) {
        if (world == null || machine == null) return;
        String tag = tag(machine.getPos()); Text expected = label(machine);
        String expectedJson = Text.Serialization.toJsonString(expected, world.getRegistryManager());
        Box search = new Box(machine.getPos()).expand(1.4D,0.0D,1.4D).offset(0.0D,3.0D,0.0D).stretch(0.0D,2.4D,0.0D);
        List<DisplayEntity.TextDisplayEntity> owned=new ArrayList<>(), unowned=new ArrayList<>(); boolean legacyTitleFound=false;
        for (DisplayEntity.TextDisplayEntity display : world.getEntitiesByType(EntityType.TEXT_DISPLAY, search, entity -> true)) {
            if (display.getCommandTags().contains(tag)) { owned.add(display); continue; }
            if (isProtectedHologram(display)) continue; unowned.add(display); if (isLegacyGachaLabel(display)) legacyTitleFound=true;
        }
        if (legacyTitleFound) for (DisplayEntity.TextDisplayEntity display : unowned) display.discard();
        DisplayEntity.TextDisplayEntity current=owned.isEmpty()?null:owned.getFirst();
        for(int i=1;i<owned.size();i++) owned.get(i).discard();
        if(current!=null && matchesText(current,expectedJson)) return; if(current!=null) current.discard();
        spawn(world,machine.getPos(),tag,expected);
    }
    static void remove(ServerWorld world, BlockPos pos) {
        if(world==null||pos==null)return; String tag=tag(pos);
        Box search=new Box(pos).expand(1.4D,0.0D,1.4D).offset(0.0D,3.0D,0.0D).stretch(0.0D,2.4D,0.0D);
        for(DisplayEntity.TextDisplayEntity d:world.getEntitiesByType(EntityType.TEXT_DISPLAY,search,e->e.getCommandTags().contains(tag))) d.discard();
    }
    private static Text label(GachaMachineBlockEntity machine) {
        int c=machine.isEmiThemed()?0xFFA6E4:0x8DEBFF; String title=machine.isEmiThemed()?"✦ LEGENDARIO DE EMI ✦":"✦ GASHA NORMAL ✦";
        MutableText text=Text.literal(title).setStyle(Style.EMPTY.withColor(c));
        if(machine.isEmiThemed()){ String name=machine.getFeaturedPokemonName(); if(name==null||name.isBlank()) name="Sincronizando…"; text.append(Text.literal("\n"+name).setStyle(Style.EMPTY.withColor(0xFFFFFF))); }
        return text;
    }
    private static void spawn(ServerWorld world, BlockPos pos, String tag, Text text) {
        DisplayEntity.TextDisplayEntity d=EntityType.TEXT_DISPLAY.create(world); if(d==null)return;
        double x=pos.getX()+0.5D,y=pos.getY()+LABEL_Y,z=pos.getZ()+0.5D; d.refreshPositionAndAngles(x,y,z,0,0); d.setNoGravity(true); d.setInvulnerable(true); d.setSilent(true); d.ignoreCameraFrustum=true;
        NbtCompound n=new NbtCompound(); d.writeNbt(n); n.putString("text",Text.Serialization.toJsonString(text,world.getRegistryManager())); n.putString("billboard","center"); n.putFloat("view_range",4.0F); n.putFloat("width",3.0F); n.putFloat("height",1.0F); n.putInt("line_width",220); n.putByte("text_opacity",(byte)0xFF); n.putByte("shadow",(byte)1); n.putByte("see_through",(byte)1); n.putByte("default_background",(byte)0); n.putInt("background",0);
        NbtCompound b=new NbtCompound(); b.putInt("block",15); b.putInt("sky",15); n.put("brightness",b); d.readNbt(n); d.refreshPositionAndAngles(x,y,z,0,0); d.ignoreCameraFrustum=true; d.addCommandTag(tag); d.setCustomName(Text.literal("Emipokemon gacha label")); d.setCustomNameVisible(false); world.spawnEntity(d);
    }
    private static boolean matchesText(DisplayEntity.TextDisplayEntity d,String expected){NbtCompound n=new NbtCompound();d.writeNbt(n);return expected.equals(n.getString("text"));}
    private static boolean isProtectedHologram(DisplayEntity.TextDisplayEntity d){for(String t:d.getCommandTags())if(t.startsWith("emipokemon:hologram:"))return true;return false;}
    private static boolean isLegacyGachaLabel(DisplayEntity.TextDisplayEntity d){if(isProtectedHologram(d))return false;for(String t:d.getCommandTags())if(t.startsWith(TAG_PREFIX))return false;NbtCompound n=new NbtCompound();d.writeNbt(n);String s=n.getString("text").toLowerCase(java.util.Locale.ROOT);return s.contains("pokémon de temporada")||s.contains("pokemon de temporada")||s.contains("legendario de emi")||s.contains("gasha normal");}
    private static String tag(BlockPos p){return TAG_PREFIX+p.getX()+":"+p.getY()+":"+p.getZ();}
}
''')

p='src/main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java'; s=read(p)
for line in ['import net.minecraft.entity.EntityType;\n','import net.minecraft.entity.decoration.DisplayEntity;\n','import net.minecraft.util.math.Box;\n','    private boolean legacySeasonDisplaysPurged;\n']: s=s.replace(line,'')
s=s.replace('''            if (!machine.legacySeasonDisplaysPurged && staggered % 40L == 0L) {\n                machine.purgeLegacySeasonDisplays(serverWorld);\n            }''','''            if (staggered % 40L == 0L) {\n                GachaMachineTextDisplayService.reconcile(serverWorld, machine);\n            }''')
s=s.replace('''    public void markRemoved() {\n        cancelPreparedReservation();\n        super.markRemoved();\n    }''','''    public void markRemoved() {\n        cancelPreparedReservation();\n        if (world instanceof ServerWorld serverWorld) GachaMachineTextDisplayService.remove(serverWorld, pos);\n        super.markRemoved();\n    }''')
s=s.replace('        nbt.putBoolean("LegacySeasonDisplaysPurged", legacySeasonDisplaysPurged);\n','').replace('        legacySeasonDisplaysPurged = nbt.contains("LegacySeasonDisplaysPurged") && nbt.getBoolean("LegacySeasonDisplaysPurged");\n','')
start=s.find('    private void purgeLegacySeasonDisplays(ServerWorld serverWorld) {')
if start!=-1: s=s[:start]+s[s.find('    private static String readableName',start):]
write(p,s)

p='src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java'; s=read(p)
start=s.index('        String displayName = machine.getFeaturedPokemonName();'); end=s.index('        if (speciesId.isBlank()) return;',start)
s=s[:start]+'        MinecraftClient client = MinecraftClient.getInstance();\n        Quaternionf cameraRotation = new Quaternionf(client.getEntityRenderDispatcher().getRotation());\n'+s[end:]
start=s.find('    private static void drawTextLine(')
if start!=-1:
    norm=s.find('    private static String normalizeSpecies',start); s=s[:start]+s[norm:]; r=s.find('    private static String readableName(')
    if r!=-1:s=s[:r]+'}\n'
write(p,s)

simple={
'src/test/java/com/emipokemon/alpha68/Alpha68EmiToolsAndSeasonDisplayRegressionTest.java':[('assertTrue(renderer.contains("GASHA NORMAL"));','assertTrue(source("main/java/com/emipokemon/gacha/machine/GachaMachineTextDisplayService.java").contains("GASHA NORMAL"));')],
'src/test/java/com/emipokemon/alpha70/Alpha70VisualRateRegressionTest.java':[('assertTrue(renderer.indexOf("drawTextLine") < renderer.indexOf("renderPokemonItem(machine"));','assertFalse(renderer.contains("drawTextLine"));\n        assertTrue(source("main/java/com/emipokemon/gacha/machine/GachaMachineTextDisplayService.java").contains("EntityType.TEXT_DISPLAY"));')],
'src/test/java/com/emipokemon/Alpha77VisualRegressionTest.java':[('assertTrue(s.contains("4.18D"));\n        assertTrue(s.contains("4.52D + bob"));','String labels=read("src/main/java/com/emipokemon/gacha/machine/GachaMachineTextDisplayService.java");\n        assertTrue(s.contains("4.52D + bob"));\n        assertTrue(labels.contains("LABEL_Y = 3.92D"));')],
'src/test/java/com/emipokemon/alpha79/Alpha79ArmorGachaAndThirdPersonRegressionTest.java':[('String r=source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java"); assertTrue(r.contains("\\"GASHA NORMAL\\"")); assertFalse(r.contains("POKÉMON DE TEMPORADA")); assertTrue(r.contains("4.52D + bob")); assertTrue(r.contains("1.35F : 1.25F"));','String r=source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java"), l=source("main/java/com/emipokemon/gacha/machine/GachaMachineTextDisplayService.java"); assertTrue(l.contains("GASHA NORMAL")); assertFalse(l.contains("POKÉMON DE TEMPORADA")); assertTrue(r.contains("4.52D + bob")); assertTrue(r.contains("1.35F : 1.25F")); assertFalse(r.contains("drawTextLine"));')]
}
for rel,pairs in simple.items():
    s=read(rel)
    for old,new in pairs:
        if old not in s: raise SystemExit(f'missing regression pattern in {rel}')
        s=s.replace(old,new)
    write(rel,s)

p='src/test/java/com/emipokemon/alpha71/Alpha71VisualSyncRegressionTest.java'; s=read(p)
old='''    @Test\n    void seasonalLabelIsAbovePhysicalCabinet() throws Exception {\n        String renderer = source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java");\n        assertTrue(renderer.contains("4.18D"));\n        assertTrue(renderer.contains("4.52D + bob"));\n        assertTrue(renderer.contains("Sincronizando…"));\n        assertTrue(renderer.indexOf("drawTextLine") < renderer.indexOf("if (speciesId.isBlank()) return"));\n    }'''
new='''    @Test\n    void seasonalLabelIsOwnedByVanillaTextDisplay() throws Exception {\n        String renderer = source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java");\n        String labels = source("main/java/com/emipokemon/gacha/machine/GachaMachineTextDisplayService.java");\n        assertFalse(renderer.contains("drawTextLine")); assertTrue(renderer.contains("4.52D + bob")); assertTrue(labels.contains("EntityType.TEXT_DISPLAY")); assertTrue(labels.contains("GASHA NORMAL")); assertTrue(labels.contains("LEGENDARIO DE EMI")); assertTrue(labels.contains("Sincronizando…")); assertTrue(labels.contains("LABEL_Y = 3.92D"));\n    }'''
if old not in s: raise SystemExit('alpha71 test pattern missing')
write(p,s.replace(old,new))

p='src/test/java/com/emipokemon/alpha75/Alpha75WorldPokemonDisplayRegressionTest.java'; s=read(p)
old='''    @Test void approvedAlpha71WorldTextPlacementIsUntouched() throws Exception {\n        String renderer = source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java");\n        assertTrue(renderer.contains("drawTextLine(matrices, vertices, textRenderer, cameraRotation, 4.18D"));\n        assertTrue(renderer.contains("matrices.translate(0.5D, 4.52D + bob, 0.5D)"));\n        assertTrue(renderer.contains("Sincronizando…"));\n    }'''
new='''    @Test void worldTextUsesServerOwnedVanillaDisplayWhilePokemonKeepsApprovedPlacement() throws Exception {\n        String renderer=source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java"); String labels=source("main/java/com/emipokemon/gacha/machine/GachaMachineTextDisplayService.java"); assertFalse(renderer.contains("drawTextLine")); assertTrue(renderer.contains("matrices.translate(0.5D, 4.52D + bob, 0.5D)")); assertTrue(labels.contains("Sincronizando…")); assertTrue(labels.contains("EntityType.TEXT_DISPLAY"));\n    }'''
if old not in s: raise SystemExit('alpha75 test pattern missing')
write(p,s.replace(old,new))

p='src/test/java/com/emipokemon/alpha80/Alpha80ArmorAndGachaCleanupRegressionTest.java'; s=read(p)
old='''    @Test\n    void legacyGachaDisplaysArePurgedOncePerMachine() throws Exception {\n        String src = source("main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java");\n        assertTrue(src.contains("legacySeasonDisplaysPurged"));\n        assertTrue(src.contains("purgeLegacySeasonDisplays"));\n        assertTrue(src.contains("EntityType.TEXT_DISPLAY"));\n    }'''
new='''    @Test\n    void legacyGachaDisplaysAreReplacedByTaggedVanillaDisplay() throws Exception {\n        String machine=source("main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java"), labels=source("main/java/com/emipokemon/gacha/machine/GachaMachineTextDisplayService.java"); assertTrue(machine.contains("GachaMachineTextDisplayService.reconcile")); assertTrue(labels.contains("EntityType.TEXT_DISPLAY")); assertTrue(labels.contains("emipokemon:gacha-label:")); assertTrue(labels.contains("isLegacyGachaLabel"));\n    }'''
if old not in s: raise SystemExit('alpha80 test pattern missing')
write(p,s.replace(old,new))

write('src/test/java/com/emipokemon/alpha81/Alpha81VanillaArmorAndServerLabelsRegressionTest.java', r'''package com.emipokemon.alpha81;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class Alpha81VanillaArmorAndServerLabelsRegressionTest {
 private static String source(String r)throws Exception{return Files.readString(Path.of("src",r));}
 @Test void versionIsAlpha81()throws Exception{assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.81"));assertTrue(source("main/java/com/emipokemon/Emipokemon.java").contains("0.4.0-alpha.81"));}
 @Test void serverLabels()throws Exception{String l=source("main/java/com/emipokemon/gacha/machine/GachaMachineTextDisplayService.java"),m=source("main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java"),r=source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java");assertTrue(l.contains("EntityType.TEXT_DISPLAY.create(world)"));assertTrue(l.contains("GASHA NORMAL"));assertTrue(l.contains("LEGENDARIO DE EMI"));assertTrue(l.contains("emipokemon:gacha-label:"));assertTrue(l.contains("emipokemon:hologram:"));assertTrue(m.contains("GachaMachineTextDisplayService.reconcile"));assertFalse(r.contains("drawTextLine"));}
 @Test void officialArmorGenerator()throws Exception{String s=Files.readString(Path.of("ci/alpha81_vanilla_armor.py"));assertTrue(s.contains("piston-meta.mojang.com"));assertTrue(s.contains("resources.download.minecraft.net"));assertTrue(s.contains("diamond_layer_1.png"));assertTrue(s.contains("hashlib.sha1"));}
 @Test void armorTransparency()throws Exception{for(int layer=1;layer<=2;layer++){BufferedImage i=ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/models/armor/emi_layer_"+layer+".png").toFile());assertEquals(64,i.getWidth());assertEquals(32,i.getHeight());int v=0,t=0;for(int y=0;y<i.getHeight();y++)for(int x=0;x<i.getWidth();x++){if(((i.getRGB(x,y)>>>24)&255)==0)t++;else v++;}assertTrue(t>500);assertTrue(v>100&&v<1500);}}
}
''')
write('CHANGELOG-0.4.0-alpha.81.md','# Emipokemon 0.4.0-alpha.81\n\n- Gacha labels are now server-owned vanilla text_display entities.\n- Normal: GASHA NORMAL only; Emi: LEGENDARIO DE EMI plus current name.\n- Armor uses official Minecraft 1.21.1 diamond UVs recolored to Emi palette with SHA-1 verified assets.\n- Armor stats and approved gacha art remain unchanged.\n')
write('GUIA-PRUEBAS-0.4.0-alpha.81.md','# Guía de pruebas — alpha.81\n\n1. Instala el mismo JAR en servidor y clientes.\n2. Comprueba texto + Pokémon simultáneamente en ambas máquinas.\n3. Reinicia y revisa que no haya duplicados.\n4. Equipa la armadura: debe conservar la silueta/transparencia vanilla de diamante, recoloreada a Emi.\n')
print('alpha81 source changes applied')
