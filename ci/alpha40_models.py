from pathlib import Path
import json

root=Path('.')

WOOD=[0,0]; METAL=[48,0]; FELT=[0,48]; LIGHT=[48,48]; STONE=[88,0]; DETAIL=[88,48]

def load(mid):
    p=root/f'src/main/resources/assets/emipokemon/geo/casino_{mid}.geo.json'
    return p,json.loads(p.read_text())

def save(p,d):
    p.write_text(json.dumps(d,indent=2)+'\n')

def bones(d):
    return {b['name']:b for b in d['minecraft:geometry'][0]['bones']}

def cube(origin,size,uv,rotation=None,pivot=None):
    d={'origin':origin,'size':size,'uv':uv}
    if rotation is not None: d['rotation']=rotation
    if pivot is not None: d['pivot']=pivot
    return d

# SLOT -----------------------------------------------------------------------
p,d=load('slot'); b=bones(d)
b['root']['cubes'].extend([
    # exact two-block cap and layered cornice
    cube([-6.45,31.0,-4.0],[12.9,1.0,8.0],METAL),
    cube([-5.85,30.55,-4.62],[11.7,0.45,0.55],LIGHT),
    # stacked front rails make the body read like slab/block construction
    cube([-5.75,21.15,-4.55],[11.5,0.5,0.45],METAL),
    cube([-5.55,10.35,-4.58],[11.1,0.55,0.48],METAL),
    # coin panel and two chunky controls
    cube([-4.6,7.1,-4.95],[4.8,1.65,0.55],STONE),
    cube([1.0,7.25,-5.05],[1.65,1.15,0.65],DETAIL),
    cube([3.15,7.25,-5.05],[1.65,1.15,0.65],LIGHT),
    # payout tray with a lip rather than a painted rectangle
    cube([-4.7,3.85,-5.15],[9.4,0.45,0.65],METAL),
    cube([-3.95,3.15,-5.45],[7.9,0.7,0.42],STONE),
])
save(p,d)

# CHIP EXCHANGE --------------------------------------------------------------
p,d=load('chip_exchange'); b=bones(d)
b['root']['cubes'].extend([
    cube([-6.55,30.8,-4.05],[13.1,1.2,8.1],WOOD),
    cube([-5.8,30.35,-4.62],[11.6,0.45,0.58],LIGHT),
    # side pilasters / copper-like framing
    cube([-6.2,12.8,-4.08],[0.55,17.5,0.5],METAL),
    cube([5.65,12.8,-4.08],[0.55,17.5,0.5],METAL),
    # keypad / acceptor cluster
    cube([-4.65,13.4,-4.72],[2.5,1.55,0.55],STONE),
    cube([-1.45,13.4,-4.72],[2.2,1.55,0.55],DETAIL),
    cube([1.55,13.4,-4.72],[3.0,1.55,0.55],LIGHT),
    # deep chip collection tray
    cube([-4.55,4.85,-5.15],[9.1,0.55,0.75],WOOD),
    cube([-3.75,4.15,-5.45],[7.5,0.72,0.48],STONE),
])
save(p,d)

# TICKET EXCHANGE ------------------------------------------------------------
p,d=load('ticket_exchange'); b=bones(d)
b['root']['cubes'].extend([
    cube([-6.5,30.7,-4.0],[13.0,1.3,8.0],METAL),
    cube([-5.7,30.3,-4.58],[11.4,0.4,0.55],LIGHT),
    cube([-6.05,12.9,-4.02],[0.55,17.0,0.48],WOOD),
    cube([5.50,12.9,-4.02],[0.55,17.0,0.48],WOOD),
    # ticket-count display and physical action button
    cube([-4.45,13.7,-4.7],[5.0,1.55,0.55],STONE),
    cube([1.35,13.7,-4.8],[2.9,1.55,0.65],DETAIL),
    # collection bin frame
    cube([-4.65,4.9,-5.08],[9.3,0.5,0.68],METAL),
    cube([-3.9,4.15,-5.4],[7.8,0.75,0.42],STONE),
])
save(p,d)

# ROULETTE -------------------------------------------------------------------
p,d=load('roulette'); b=bones(d)
# Number/betting board on the dealer-facing side of the wheel.
b['root']['cubes'].extend([
    cube([-6.2,8.02,2.15],[1.45,0.18,1.65],LIGHT),
    cube([-4.35,8.02,2.15],[1.45,0.18,1.65],DETAIL),
    cube([-2.50,8.02,2.15],[1.45,0.18,1.65],LIGHT),
    cube([1.05,8.02,2.15],[1.45,0.18,1.65],DETAIL),
    cube([2.90,8.02,2.15],[1.45,0.18,1.65],LIGHT),
    cube([4.75,8.02,2.15],[1.25,0.18,1.65],DETAIL),
    cube([-6.2,8.02,4.05],[12.2,0.18,0.45],METAL),
])
save(p,d)

# POKER ----------------------------------------------------------------------
p,d=load('poker'); b=bones(d)
# Chip stacks, dealer button and two inset betting spots.
b['root']['cubes'].extend([
    cube([-5.8,8.02,2.45],[1.05,0.52,1.05],DETAIL),
    cube([-5.72,8.52,2.53],[0.89,0.42,0.89],METAL),
    cube([4.65,8.02,2.45],[1.05,0.52,1.05],LIGHT),
    cube([4.73,8.52,2.53],[0.89,0.42,0.89],DETAIL),
    cube([-0.62,8.02,3.0],[1.24,0.30,1.24],LIGHT),
    cube([-5.65,8.0,-3.2],[2.3,0.16,1.35],STONE),
    cube([3.35,8.0,-3.2],[2.3,0.16,1.35],STONE),
])
save(p,d)

# BLACKJACK ------------------------------------------------------------------
p,d=load('blackjack'); b=bones(d)
# Five player betting pads plus a more obvious dealer shoe and discard tray.
for x in (-5.4,-2.7,0.0,2.7,5.4):
    b['root']['cubes'].append(cube([x-0.65,8.0,-3.25],[1.3,0.16,1.3],DETAIL if int((x+5.4)/2.7)%2 else LIGHT))
b['root']['cubes'].extend([
    cube([4.2,8.0,1.45],[2.55,1.0,3.45],STONE),
    cube([4.55,8.85,1.85],[1.85,0.42,2.35],LIGHT),
    cube([-6.25,8.0,2.1],[2.1,0.55,2.35],METAL),
    cube([-5.85,8.5,2.45],[1.3,0.25,1.65],LIGHT),
])
save(p,d)

# DICE / CRAPS ---------------------------------------------------------------
p,d=load('dice'); b=bones(d)
# Table zones and rail blocks make the layout feel like a built craps table.
b['root']['cubes'].extend([
    cube([-5.9,8.02,-5.6],[3.2,0.16,2.25],LIGHT),
    cube([-1.6,8.02,-5.6],[3.2,0.16,2.25],DETAIL),
    cube([2.7,8.02,-5.6],[3.2,0.16,2.25],LIGHT),
    cube([-5.9,8.02,3.35],[3.2,0.16,2.25],DETAIL),
    cube([-1.6,8.02,3.35],[3.2,0.16,2.25],LIGHT),
    cube([2.7,8.02,3.35],[3.2,0.16,2.25],DETAIL),
    cube([-6.7,8.08,-0.38],[13.4,0.18,0.76],METAL),
])
save(p,d)
