from pathlib import Path
root=Path('.')

def repl(path, old, new, label):
    p=root/path
    s=p.read_text()
    if old not in s:
        raise SystemExit(f'missing alpha45 anchor: {label}')
    p.write_text(s.replace(old,new,1))

repl('gradle.properties','mod_version=0.4.0-alpha.44','mod_version=0.4.0-alpha.45','gradle version')
repl('src/main/java/com/emipokemon/Emipokemon.java','0.4.0-alpha.44','0.4.0-alpha.45','core version')

(root/'CHANGELOG-0.4.0-alpha.45.md').write_text('''# Emipokemon 0.4.0-alpha.45

Implementable Pokémon-world roulette presentation pass based on alpha.44.

- Keeps the alpha.44 server-authoritative roulette rules, payouts, anti-duplicate reservation and multiplayer round flow.
- Rebuilds the roulette screen to match the approved pixel-casino mockup using only Minecraft draw primitives and existing widgets.
- Moves the help text away from the wheel so the roulette stays fully visible.
- Adds a real server-backed five-result roulette history per physical table/session.
- Adds functional bottom denomination chips that change the same real wager field as the side quick buttons.
- Keeps every betting cloth cell clickable and connected to the existing server action path.
- Shows real round status/timer, player participants, balance, selected bet and table state in framed sections.
- Adds original capture-orb-inspired casino decoration without introducing external image assets.
- Preserves the alpha.44 no-blur contract.
- Other casino games remain functionally unchanged and continue using the shared generic casino shell.

Real Cobbleverse visual and two-client validation is required before promotion.
''')
