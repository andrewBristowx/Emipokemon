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
        raise SystemExit(f'missing alpha38 anchor: {label}')
    return text.replace(old, new, 1)

p='gradle.properties'
s=read(p)
s=replace_once(s,'mod_version=0.4.0-alpha.37','mod_version=0.4.0-alpha.38','gradle version')
write(p,s)

p='src/main/java/com/emipokemon/Emipokemon.java'
s=read(p)
s=replace_once(s,'0.4.0-alpha.37','0.4.0-alpha.38','core version')
write(p,s)

write('CHANGELOG-0.4.0-alpha.38.md', '''# Emipokemon 0.4.0-alpha.38\n\nCasino silhouette redesign candidate.\n\n- Keeps the server-authoritative multiplayer casino backend from alpha.36/37 unchanged.\n- Redesigns all seven casino models so each game is recognizable by silhouette and functional details instead of sharing generic box/table geometry.\n- Slot: recessed three-reel cabinet, marquee, control deck, payout tray and side lever.\n- Chip exchange: changer kiosk with display, chip carousel/hopper and dispensing tray.\n- Ticket exchange: redemption kiosk with ticket mouth, protruding ticket strip and collection tray.\n- Roulette: raised octagonal-style table, visible wheel/ring/spokes, hub and ball track.\n- Poker: compact oval-style felt table with community-card lane and chip/dealer details.\n- Blackjack: dealer-oriented curved/winged table with card shoe and player betting positions.\n- Dice: craps-inspired recessed tray with raised rails and two visible dice.\n- Keeps every cube inside one Minecraft block footprint/height and preserves the GeckoLib animation bone names.\n- Keeps the alpha.37 sharp casino GUI and brighter texture family.\n- Does not modify EmiProtecciones or Arlight.\n\nReal Cobbleverse visual validation is still required.\n''')
