from pathlib import Path

root=Path('.')

def replace_once(path,old,new,label):
    p=root/path
    s=p.read_text()
    if old not in s:
        raise SystemExit(f'missing alpha42 anchor: {label}')
    p.write_text(s.replace(old,new,1))

replace_once('gradle.properties','mod_version=0.4.0-alpha.40','mod_version=0.4.0-alpha.42','gradle version')
replace_once('src/main/java/com/emipokemon/Emipokemon.java','0.4.0-alpha.40','0.4.0-alpha.42','core version')

(root/'CHANGELOG-0.4.0-alpha.42.md').write_text('''# Emipokemon 0.4.0-alpha.42\n\nConnected casino geometry and decorated game-piece pass.\n\n- Starts cleanly from alpha.40; no unrelated alpha.41 GUI experiment is included.\n- Keeps server-authoritative multiplayer, economy, casino networking, GUI, holograms and anti-duplicate protections unchanged.\n- Removes tiny air gaps between front trim and cabinet bodies by forcing overlap with their supports.\n- Adds continuous rear shells and structural ribs to slot, chip exchange and ticket exchange so stacked sections remain visibly connected from the back and sides.\n- Replaces blank-looking marquee/control faces with layered machine-specific panels.\n- Adds physical rank/suit markers to poker and blackjack cards.\n- Adds physical dark pips to the animated dice, preserving the existing dice animation bone and rotations.\n- Adds markings to roulette, blackjack and craps betting zones so pale surfaces no longer read as unfinished white parts.\n- Keeps the block-inspired pixel material atlas from alpha.39/40.\n- Does not modify EmiProtecciones or Arlight.\n\nReal Cobbleverse visual validation is still required.\n''')
