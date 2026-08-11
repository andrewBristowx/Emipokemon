from pathlib import Path
root=Path('.')
for rel in ['gradle.properties','src/main/java/com/emipokemon/Emipokemon.java']:
    p=root/rel
    s=p.read_text().replace('0.4.0-alpha.45','0.4.0-alpha.46')
    p.write_text(s)
(root/'CHANGELOG-0.4.0-alpha.46.md').write_text("""# Emipokemon 0.4.0-alpha.46

Asset-backed roulette visual pass based on alpha.45.

- Converts the approved roulette concept into real in-game GUI assets.
- Uses separate header, left-table chrome, right information chrome, wheel trim and EMI Casino medallion textures.
- Dynamic/server-authoritative information remains live: balance, countdown, amount, players, recent results, selected bet and shared roulette result.
- Betting hitboxes are remapped to the visible asset table and the bottom/side quick chips remain functional.
- Keeps alpha.45 payouts, anti-dupe, multiplayer authority, history and networking unchanged.
- Keeps the alpha.44 no-blur contract.
- Other casino games remain on their existing functional GUI until their dedicated visual passes.

Real Cobbleverse visual validation is required.
""")
