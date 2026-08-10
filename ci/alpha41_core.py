from pathlib import Path

root=Path('.')

def replace_once(path,old,new,label):
    p=root/path
    s=p.read_text()
    if old not in s:
        raise SystemExit(f'missing alpha41 anchor: {label}')
    p.write_text(s.replace(old,new,1))

replace_once('gradle.properties','mod_version=0.4.0-alpha.40','mod_version=0.4.0-alpha.41','gradle version')
replace_once('src/main/java/com/emipokemon/Emipokemon.java','0.4.0-alpha.40','0.4.0-alpha.41','core version')

(root/'CHANGELOG-0.4.0-alpha.41.md').write_text('''# Emipokemon 0.4.0-alpha.41\n\nCasino interface polish pass.\n\n- Starts from the complete alpha.40 casino implementation and keeps all server-authoritative multiplayer/economy logic unchanged.\n- Rebuilds the casino screen into a cleaner two-card layout with a strong header, game-specific accent colors, readable balance/phase badges and a dedicated result/message strip.\n- Makes shared table games explicitly identify themselves as multiplayer tables and keeps personal service machines clearly separated.\n- Adds quick bet amount controls for minimum, x5 and x10 without changing server validation.\n- Improves roulette exact-number placement, action grouping, player-list readability and table/private-state hierarchy.\n- Keeps the alpha.40 two-block cabinet models, table models, original casino material atlas and GeckoLib animation bones unchanged.\n- Preserves the alpha.37 no-blur rendering fix so the GUI remains sharp in Cobbleverse.\n- Does not modify EmiProtecciones or Arlight.\n\nReal Cobbleverse visual validation is still required before this candidate can be considered finished.\n''')
