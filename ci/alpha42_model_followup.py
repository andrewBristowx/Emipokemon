from pathlib import Path
import json
p=Path('src/main/resources/assets/emipokemon/geo/casino_blackjack.geo.json')
d=json.loads(p.read_text())
b={x['name']:x for x in d['minecraft:geometry'][0]['bones']}
b['root']['cubes'].append({'origin':[-2.4,8.0,3.65],'size':[4.8,0.28,0.95],'uv':[88,48]})
p.write_text(json.dumps(d,indent=2)+'\n')
