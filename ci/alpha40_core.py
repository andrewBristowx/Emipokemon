from pathlib import Path

root=Path('.')

def replace_once(path,old,new,label):
    p=root/path
    s=p.read_text()
    if old not in s:
        raise SystemExit(f'missing alpha40 anchor: {label}')
    p.write_text(s.replace(old,new,1))

replace_once('gradle.properties','mod_version=0.4.0-alpha.39','mod_version=0.4.0-alpha.40','gradle version')
replace_once('src/main/java/com/emipokemon/Emipokemon.java','0.4.0-alpha.39','0.4.0-alpha.40','core version')

(root/'CHANGELOG-0.4.0-alpha.40.md').write_text('''# Emipokemon 0.4.0-alpha.40\n\nCasino construction-detail pass.\n\n- Starts from the complete alpha.39 server-authoritative multiplayer implementation.\n- Keeps casino gameplay, GUI networking, economy, holograms and anti-duplicate protections unchanged.\n- Brings slot, chip exchange and ticket exchange to essentially two full Minecraft blocks of visual height.\n- Adds layered slab-like caps, framed fronts, inset panels, buttons, trays and side rails so the cabinets read as assembled voxel furniture instead of smooth boxes.\n- Adds game-specific tabletop hardware to roulette, poker, blackjack and dice while keeping all tables below one block of visual height except movable game pieces.\n- Continues to use the original alpha.39 material atlas: plank-like wood, copper/brass-like metal, emerald-felt-like surface, quartz/iron-like light panels and stone-like bases. These are original pixel patterns, not redistributed vanilla Minecraft textures.\n- Preserves all GeckoLib animation bone names and the extended casino renderer bounds from alpha.39.\n- Does not modify EmiProtecciones or Arlight.\n\nReal Cobbleverse visual validation is still required before this candidate can be considered finished.\n''')
