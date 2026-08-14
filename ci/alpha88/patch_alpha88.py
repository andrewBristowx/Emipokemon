import json
from pathlib import Path
p=Path('src/main/resources/assets/emipokemon/geo/armor/emi_armor.geo.json')
data=json.loads(p.read_text())
for bone in data['minecraft:geometry'][0]['bones']:
    if bone['name']=='armorHead':
        bone['cubes'] = bone['cubes'][9:]
    elif bone['name']=='helmet_heart':
        bone['cubes']=[{
            'origin':[-1.7, 28.2, -5.38],
            'size':[3.4, 2.4, 0.18],
            'uv': {k:{'uv':[16,0],'uv_size':[16,16]} for k in ['north','south','east','west','up','down']}
        }]
    elif bone['name']=='heart_core':
        bone['cubes']=[{
            'origin':[-2.15, 17.3, -3.42],
            'size':[4.3, 3.4, 0.18],
            'uv': {k:{'uv':[16,0],'uv_size':[16,16]} for k in ['north','south','east','west','up','down']}
        }]
p.write_text(json.dumps(data, indent=2) + '\n')
