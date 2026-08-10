from pathlib import Path
import json

root=Path('.')
WOOD=[0,0]; METAL=[48,0]; FELT=[0,48]; LIGHT=[48,48]; STONE=[88,0]; DETAIL=[88,48]

def load(mid):
    p=root/f'src/main/resources/assets/emipokemon/geo/casino_{mid}.geo.json'
    return p,json.loads(p.read_text())

def save(p,d): p.write_text(json.dumps(d,indent=2)+'\n')
def bones(d): return {b['name']:b for b in d['minecraft:geometry'][0]['bones']}
def cube(origin,size,uv,rotation=None,pivot=None):
    d={'origin':origin,'size':size,'uv':uv}
    if rotation is not None: d['rotation']=rotation
    if pivot is not None: d['pivot']=pivot
    return d

# Tall cabinets ---------------------------------------------------------------
# Continuous rear shells make all stacked cabinet sections read as one built object from every side.
# Front plaques overlap their backing by >=0.10 Gecko units so no decorative piece can visibly float.
for mid,back_uv in [('slot',WOOD),('chip_exchange',METAL),('ticket_exchange',WOOD)]:
    p,d=load(mid); b=bones(d)
    b['root']['cubes'].append(cube([-5.55,1.2,3.25],[11.1,30.15,0.72],back_uv))
    # rear vertical ribs make the casing feel assembled from blocks rather than a flat plate
    b['root']['cubes'].append(cube([-5.85,1.0,3.32],[0.65,30.7,0.78],METAL))
    b['root']['cubes'].append(cube([5.20,1.0,3.32],[0.65,30.7,0.78],METAL))
    save(p,d)

# Slot: replace blank marquee/control faces with a recessed, decorated game panel.
p,d=load('slot'); b=bones(d)
b['root']['cubes'].extend([
    cube([-5.2,27.0,-4.76],[10.4,2.75,0.78],DETAIL),
    cube([-4.55,27.45,-5.02],[2.25,1.65,0.34],LIGHT),
    cube([-1.12,27.45,-5.02],[2.25,1.65,0.34],METAL),
    cube([2.30,27.45,-5.02],[2.25,1.65,0.34],LIGHT),
    cube([-4.95,30.45,-4.55],[9.9,0.65,0.62],METAL),
    # attached button faces, both colored; no blank white control
    cube([3.10,7.28,-5.16],[1.75,1.12,0.22],DETAIL),
])
# Pull alpha40's top light strip inward so it overlaps its cap instead of floating 0.07 forward.
for c in b['root']['cubes']:
    if c.get('origin')==[-5.85,30.55,-4.62]:
        c['origin']=[-5.85,30.55,-4.50]; c['size']=[11.7,0.45,0.68]
save(p,d)

# Chip exchange: illuminated MICHI-token style plaque, keypad and hopper window.
p,d=load('chip_exchange'); b=bones(d)
b['root']['cubes'].extend([
    cube([-5.15,26.75,-4.76],[10.3,2.85,0.76],DETAIL),
    cube([-3.85,27.30,-5.00],[7.7,1.70,0.30],METAL),
    cube([-2.8,27.72,-5.13],[1.05,0.85,0.20],LIGHT),
    cube([-0.52,27.72,-5.13],[1.05,0.85,0.20],LIGHT),
    cube([1.76,27.72,-5.13],[1.05,0.85,0.20],LIGHT),
    # keypad buttons physically inset into the control block
    cube([-4.32,13.62,-4.93],[0.52,0.48,0.30],DETAIL),
    cube([-3.55,13.62,-4.93],[0.52,0.48,0.30],LIGHT),
    cube([-2.78,13.62,-4.93],[0.52,0.48,0.30],DETAIL),
])
for c in b['root']['cubes']:
    if c.get('origin')==[-5.8,30.35,-4.62]:
        c['origin']=[-5.8,30.35,-4.48]; c['size']=[11.6,0.45,0.72]
save(p,d)

# Ticket exchange: ticket motif and a non-white display/collection face.
p,d=load('ticket_exchange'); b=bones(d)
b['root']['cubes'].extend([
    cube([-5.05,26.55,-4.76],[10.1,2.95,0.76],DETAIL),
    cube([-4.10,27.15,-5.02],[8.20,1.75,0.32],LIGHT),
    # perforation/accent marks on the ticket sign
    cube([-3.50,27.68,-5.16],[0.55,0.55,0.18],DETAIL),
    cube([-1.45,27.68,-5.16],[0.55,0.55,0.18],DETAIL),
    cube([0.60,27.68,-5.16],[0.55,0.55,0.18],DETAIL),
    cube([2.65,27.68,-5.16],[0.55,0.55,0.18],DETAIL),
    cube([-4.15,16.78,-4.62],[8.3,3.15,0.24],DETAIL),
])
for c in b['root']['cubes']:
    if c.get('origin')==[-5.7,30.3,-4.58]:
        c['origin']=[-5.7,30.3,-4.46]; c['size']=[11.4,0.4,0.68]
save(p,d)

# Poker cards -----------------------------------------------------------------
p,d=load('poker'); b=bones(d)
# Physical rank/suit pixels overlap the card tops so they read as cards rather than blank slabs.
card_specs=[
    (-4.3,-1.45,-7,[-3.55,8.15,-.45]),(-2.38,-1.58,0,None),(-0.72,-1.62,0,None),
    (0.95,-1.58,0,None),(2.86,-1.45,7,[3.58,8.15,-.45])
]
for i,(x,z,ang,piv) in enumerate(card_specs):
    rot=[0,ang,0] if ang else None
    # corner rank block + central suit block; slight overlap into the 0.18-high card
    b['cards']['cubes'].append(cube([x+0.18,8.19,z+0.16],[0.34,0.09,0.38],DETAIL if i%2==0 else STONE,rot,piv))
    b['cards']['cubes'].append(cube([x+0.58,8.19,z+0.80],[0.42,0.09,0.42],STONE if i%2==0 else DETAIL,rot,piv))
save(p,d)

# Blackjack cards + betting spots --------------------------------------------
p,d=load('blackjack'); b=bones(d)
black_cards=[(-2.0,-1.35,-8,[-1.2,8.15,-.25]),(0.45,-1.35,8,[1.25,8.15,-.25])]
for i,(x,z,ang,piv) in enumerate(black_cards):
    rot=[0,ang,0]
    b['cards']['cubes'].append(cube([x+0.18,8.19,z+0.18],[0.36,0.09,0.42],DETAIL if i==0 else STONE,rot,piv))
    b['cards']['cubes'].append(cube([x+0.62,8.19,z+0.92],[0.46,0.09,0.46],STONE if i==0 else DETAIL,rot,piv))
# Small center marks keep the pale betting pads from reading as unfinished white rectangles.
for x in (-5.4,-2.7,0.0,2.7,5.4):
    b['root']['cubes'].append(cube([x-0.22,8.12,-2.82],[0.44,0.08,0.44],STONE))
save(p,d)

# Dice / craps ----------------------------------------------------------------
p,d=load('dice'); b=bones(d)
# Keep the animated dice bone but give each die physical dark pips, using the exact same rotation/pivot.
die1_rot=[9,16,-7]; die1_piv=[-2.5,10.1,-.4]
die2_rot=[-7,-18,10]; die2_piv=[2.7,10.3,2.0]
# Die 1 top: three pips; front: two pips.
for ox,oz in [(-3.55,-1.45),(-2.75,-0.65),(-1.95,0.15)]:
    b['dice']['cubes'].append(cube([ox,11.62,oz],[0.46,0.16,0.46],STONE,die1_rot,die1_piv))
for ox,oy in [(-3.55,9.15),(-1.95,10.45)]:
    b['dice']['cubes'].append(cube([ox,oy,-2.08],[0.46,0.46,0.16],STONE,die1_rot,die1_piv))
# Die 2 top: four pips; front: three pips.
for ox,oz in [(1.55,0.85),(3.35,0.85),(1.55,2.65),(3.35,2.65)]:
    b['dice']['cubes'].append(cube([ox,11.82,oz],[0.46,0.16,0.46],STONE,die2_rot,die2_piv))
for ox,oy in [(1.55,9.35),(2.45,10.25),(3.35,11.05)]:
    b['dice']['cubes'].append(cube([ox,oy,0.32],[0.46,0.46,0.16],STONE,die2_rot,die2_piv))
# Add small dark centers to the six pale/colored craps zones.
for x,z in [(-4.3,-4.65),(0,-4.65),(4.3,-4.65),(-4.3,4.0),(0,4.0),(4.3,4.0)]:
    b['root']['cubes'].append(cube([x-0.28,8.14,z-0.28],[0.56,0.08,0.56],STONE))
save(p,d)

# Roulette board --------------------------------------------------------------
p,d=load('roulette'); b=bones(d)
# Give every pale betting cell a center mark so none reads as an unfinished white tile.
for x in (-5.48,-3.63,-1.78,1.77,3.62,5.35):
    b['root']['cubes'].append(cube([x-0.24,8.15,2.72],[0.48,0.08,0.48],STONE))
save(p,d)
