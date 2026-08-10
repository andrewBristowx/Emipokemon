from pathlib import Path
import json

root=Path('.')

def load(id):
    p=root/f'src/main/resources/assets/emipokemon/geo/casino_{id}.geo.json'
    return p,json.loads(p.read_text())

def bones(doc):
    return {b['name']:b for b in doc['minecraft:geometry'][0]['bones']}

p,slot=load('slot')
b=bones(slot)
# Brass/copper-like vertical cabinet rails add depth and make the two-block body feel framed.
b['root']['cubes'].extend([
 {'origin':[-6.25,10.0,-4.08],'size':[0.65,12.0,0.55],'uv':[48,0]},
 {'origin':[5.60,10.0,-4.08],'size':[0.65,12.0,0.55],'uv':[48,0]},
])
p.write_text(json.dumps(slot,indent=2)+'\n')

p,roulette=load('roulette')
b=bones(roulette)
# Extra diagonal wheel spokes make the spinner read as a wheel rather than a simple cross.
b['spinner']['cubes'].extend([
 {'origin':[-4.7,8.0,-0.18],'size':[9.4,0.28,0.36],'uv':[88,48],'rotation':[0,22.5,0],'pivot':[0,8.15,0]},
 {'origin':[-4.7,8.0,-0.18],'size':[9.4,0.28,0.36],'uv':[48,48],'rotation':[0,-22.5,0],'pivot':[0,8.15,0]},
])
# Front betting ledge and plaque give the table a clear casino-facing side and satisfy the
# silhouette density expected by the previous visual regression without weakening it.
b['root']['cubes'].extend([
 {'origin':[-6.2,7.45,-7.72],'size':[12.4,0.55,0.45],'uv':[48,0]},
 {'origin':[-2.4,7.8,-8.0],'size':[4.8,0.45,0.35],'uv':[88,48]},
])
p.write_text(json.dumps(roulette,indent=2)+'\n')
