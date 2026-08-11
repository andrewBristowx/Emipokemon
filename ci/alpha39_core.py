from pathlib import Path

root = Path('.')

def read(rel):
    return (root / rel).read_text()

def write(rel, text):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text)

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'missing alpha39 anchor: {label}')
    return text.replace(old, new, 1)

p='gradle.properties'
s=read(p)
s=replace_once(s,'mod_version=0.4.0-alpha.38','mod_version=0.4.0-alpha.39','gradle version')
write(p,s)

p='src/main/java/com/emipokemon/Emipokemon.java'
s=read(p)
s=replace_once(s,'0.4.0-alpha.38','0.4.0-alpha.39','core version')
write(p,s)

# Tall casino cabinets intentionally render beyond the source block AABB.
p='src/client/java/com/emipokemon/client/render/CasinoMachineRenderer.java'
s=read(p)
anchor='''    @Override\n    public @Nullable RenderLayer getRenderType(CasinoMachineBlockEntity animatable, Identifier texture,\n                                               @Nullable VertexConsumerProvider buffers, float partialTick) {\n        return RenderLayer.getEntityCutoutNoCull(texture, false);\n    }\n'''
insert='''    @Override\n    public boolean rendersOutsideBoundingBox(CasinoMachineBlockEntity blockEntity) {\n        return true;\n    }\n\n    @Override\n    public int getRenderDistance() {\n        return 96;\n    }\n\n'''
if 'rendersOutsideBoundingBox(CasinoMachineBlockEntity' not in s:
    s=replace_once(s,anchor,anchor+'\n'+insert,'renderer extended bounds')
write(p,s)

write('CHANGELOG-0.4.0-alpha.39.md', '''# Emipokemon 0.4.0-alpha.39\n\nMinecraft-material casino art pass.\n\n- Keeps the server-authoritative multiplayer casino implementation and sharp alpha.37+ GUI unchanged.\n- Rebuilds slot, chip exchange and ticket exchange as roughly two-block-tall cabinets with stronger arcade/casino silhouettes.\n- Reworks roulette, poker, blackjack and dice into heavier, more substantial one-block-height casino furniture.\n- Replaces flat color fields with original pixel-art material patterns inspired by Minecraft building materials (planks, copper-like metal, gold trim, iron/quartz-like panels, emerald felt and stone-like bases) without bundling vanilla Minecraft textures.\n- Marks the GeckoLib casino renderer as rendering outside the source block bounds so tall cabinets are not culled incorrectly.\n- Preserves the existing animation bone names and multiplayer networking.\n- Does not modify EmiProtecciones or Arlight.\n\nReal Cobbleverse visual validation is still required.\n''')
