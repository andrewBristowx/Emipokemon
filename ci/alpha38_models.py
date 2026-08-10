from pathlib import Path
import json

root = Path('.')

def write(rel, text):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text)

def cube(origin, size, uv, rotation=None, pivot=None):
    value = {'origin': origin, 'size': size, 'uv': uv}
    if rotation is not None:
        value['rotation'] = rotation
    if pivot is not None:
        value['pivot'] = pivot
    return value

def bone(name, cubes, pivot=None, parent=None):
    value = {'name': name}
    if parent:
        value['parent'] = parent
    if pivot is not None:
        value['pivot'] = pivot
    if cubes:
        value['cubes'] = cubes
    return value

def geo(identifier, bones):
    return {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {
                'identifier': 'geometry.emipokemon.' + identifier,
                'texture_width': 128,
                'texture_height': 128,
                'visible_bounds_width': 2.0,
                'visible_bounds_height': 2.0,
                'visible_bounds_offset': [0, 0.75, 0]
            },
            'bones': bones
        }]
    }

BODY=[0,0]
ACCENT=[48,0]
SURFACE=[0,48]
LIGHT=[48,48]
DARK=[88,0]
DETAIL=[88,48]

models={}

# SLOT MACHINE ---------------------------------------------------------------
# A real cabinet silhouette: pedestal/base, lower payout body, projecting control deck,
# recessed reel bay, crown/marquee and a side lever. Front is negative Z.
models['casino_slot']=geo('casino_slot',[
    bone('root',[
        cube([-6.4,0.0,-3.9],[12.8,1.0,7.8],DARK),
        cube([-5.8,1.0,-3.55],[11.6,4.9,7.1],BODY),
        cube([-5.55,2.0,-4.05],[11.1,1.45,0.7],ACCENT),
        cube([-4.35,2.28,-4.58],[8.7,0.72,0.72],DARK),
        cube([-5.7,5.55,-4.75],[11.4,1.25,1.55],ACCENT),
        cube([-4.9,5.88,-5.16],[5.8,0.62,0.62],DETAIL),
        cube([2.0,5.88,-5.16],[2.0,0.62,0.62],LIGHT),
        cube([-5.35,6.45,-3.35],[10.7,6.35,6.7],BODY),
        cube([-5.05,7.15,-3.86],[10.1,4.85,0.6],DARK),
        cube([-4.65,7.55,-4.24],[9.3,4.05,0.42],SURFACE),
        cube([-6.0,12.55,-3.75],[12.0,2.35,7.5],ACCENT),
        cube([-4.75,13.05,-4.16],[9.5,1.25,0.46],LIGHT),
        cube([-4.2,14.45,-3.98],[8.4,0.45,0.42],DETAIL),
    ],pivot=[0,0,0]),
    bone('reel1',[
        cube([-4.32,7.9,-4.72],[2.35,3.35,0.58],LIGHT),
        cube([-4.05,9.18,-5.02],[1.8,0.62,0.38],DETAIL),
    ],pivot=[-3.15,9.58,-4.45],parent='root'),
    bone('reel2',[
        cube([-1.18,7.9,-4.72],[2.35,3.35,0.58],LIGHT),
        cube([-0.90,9.18,-5.02],[1.8,0.62,0.38],DETAIL),
    ],pivot=[0,9.58,-4.45],parent='root'),
    bone('reel3',[
        cube([1.97,7.9,-4.72],[2.35,3.35,0.58],LIGHT),
        cube([2.25,9.18,-5.02],[1.8,0.62,0.38],DETAIL),
    ],pivot=[3.15,9.58,-4.45],parent='root'),
    bone('lever',[
        cube([6.05,5.15,-0.55],[0.72,4.35,0.72],DARK),
        cube([5.72,9.05,-0.88],[1.38,1.38,1.38],ACCENT),
    ],pivot=[6.4,5.35,-0.2],parent='root'),
    bone('lights',[
        cube([-5.35,14.88,-3.98],[10.7,0.45,0.42],LIGHT),
        cube([-5.58,6.52,-4.88],[11.16,0.34,0.34],LIGHT),
    ],pivot=[0,14.8,-3.8],parent='root'),
])

# CHIP EXCHANGE --------------------------------------------------------------
# Changer kiosk with a large display, a central animated chip carousel and a deep tray.
models['casino_chip_exchange']=geo('casino_chip_exchange',[
    bone('root',[
        cube([-6.1,0,-3.75],[12.2,1.0,7.5],DARK),
        cube([-5.55,1.0,-3.45],[11.1,11.9,6.9],BODY),
        cube([-6.0,12.65,-3.8],[12.0,2.05,7.6],ACCENT),
        cube([-4.75,13.05,-4.18],[9.5,1.15,0.46],LIGHT),
        cube([-4.55,8.55,-3.96],[9.1,3.0,0.58],SURFACE),
        cube([-3.95,9.15,-4.32],[7.9,1.75,0.36],DARK),
        cube([-5.0,5.65,-4.55],[10.0,1.25,1.35],ACCENT),
        cube([-4.3,3.1,-4.34],[8.6,1.35,0.88],DARK),
        cube([-3.5,2.25,-4.75],[7.0,1.0,1.25],SURFACE),
        cube([-2.85,1.95,-5.05],[5.7,0.55,0.55],LIGHT),
    ],pivot=[0,0,0]),
    bone('spinner',[
        cube([-2.7,5.35,-5.05],[5.4,0.42,0.42],LIGHT),
        cube([-0.22,3.65,-5.05],[0.44,3.85,0.42],LIGHT),
        cube([-1.55,4.55,-5.34],[3.1,3.1,0.32],DETAIL),
        cube([-1.2,4.9,-5.57],[2.4,2.4,0.22],ACCENT,rotation=[0,45,0],pivot=[0,6.1,-5.45]),
    ],pivot=[0,6.1,-5.2],parent='root'),
    bone('lights',[
        cube([-5.1,13.98,-4.02],[10.2,0.42,0.38],LIGHT),
        cube([-4.55,8.45,-4.26],[9.1,0.28,0.30],LIGHT),
    ],pivot=[0,14.0,-3.8],parent='root'),
])

# TICKET EXCHANGE ------------------------------------------------------------
# Redemption kiosk: display, illuminated ticket mouth, actual protruding ticket strip and collection bin.
models['casino_ticket_exchange']=geo('casino_ticket_exchange',[
    bone('root',[
        cube([-6.0,0,-3.7],[12.0,1.0,7.4],DARK),
        cube([-5.45,1.0,-3.4],[10.9,12.0,6.8],BODY),
        cube([-6.0,12.75,-3.72],[12.0,1.95,7.44],ACCENT),
        cube([-4.8,13.16,-4.10],[9.6,1.05,0.45],LIGHT),
        cube([-4.65,8.85,-3.92],[9.3,2.6,0.58],SURFACE),
        cube([-3.95,9.35,-4.28],[7.9,1.55,0.34],DARK),
        cube([-4.9,5.55,-4.32],[9.8,1.0,0.78],ACCENT),
        cube([-3.9,5.82,-4.78],[7.8,0.44,0.52],DARK),
        cube([-4.15,2.1,-4.35],[8.3,2.25,0.95],SURFACE),
        cube([-3.4,1.75,-4.78],[6.8,0.55,0.55],LIGHT),
    ],pivot=[0,0,0]),
    bone('cards',[
        cube([-2.7,4.45,-5.0],[5.4,1.15,0.22],LIGHT),
        cube([-2.45,3.72,-5.15],[4.9,0.78,0.18],LIGHT,rotation=[12,0,0],pivot=[0,4.5,-5.0]),
        cube([-2.2,3.05,-5.25],[4.4,0.72,0.16],DETAIL,rotation=[18,0,0],pivot=[0,3.75,-5.1]),
    ],pivot=[0,4.8,-4.9],parent='root'),
    bone('lights',[
        cube([-5.0,13.93,-3.95],[10.0,0.42,0.38],LIGHT),
        cube([-4.45,5.45,-4.62],[8.9,0.26,0.28],LIGHT),
    ],pivot=[0,14.0,-3.8],parent='root'),
])

# ROULETTE -------------------------------------------------------------------
# Compact casino roulette: pedestal plus an octagonal-looking tabletop and a clearly separated wheel.
models['casino_roulette']=geo('casino_roulette',[
    bone('root',[
        cube([-4.2,0,-4.2],[8.4,6.7,8.4],BODY),
        cube([-4.9,0,-4.9],[9.8,0.9,9.8],DARK),
        cube([-5.65,6.55,-5.65],[11.3,1.05,11.3],SURFACE),
        cube([-7.0,6.85,-4.55],[14.0,0.75,9.1],SURFACE),
        cube([-4.55,6.85,-7.0],[9.1,0.75,14.0],SURFACE),
        cube([-7.3,7.45,-4.2],[14.6,0.48,0.55],ACCENT),
        cube([-7.3,7.45,3.65],[14.6,0.48,0.55],ACCENT),
        cube([-4.2,7.45,-7.3],[0.55,0.48,14.6],ACCENT),
        cube([3.65,7.45,-7.3],[0.55,0.48,14.6],ACCENT),
        cube([-5.15,7.6,-5.15],[10.3,0.45,10.3],DARK),
    ],pivot=[0,0,0]),
    bone('spinner',[
        cube([-4.65,8.02,-0.24],[9.3,0.35,0.48],ACCENT),
        cube([-0.24,8.02,-4.65],[0.48,0.35,9.3],LIGHT),
        cube([-4.25,8.02,-0.22],[8.5,0.32,0.44],LIGHT,rotation=[0,45,0],pivot=[0,8.18,0]),
        cube([-4.25,8.02,-0.22],[8.5,0.32,0.44],ACCENT,rotation=[0,-45,0],pivot=[0,8.18,0]),
        cube([-1.0,7.98,-1.0],[2.0,0.55,2.0],DETAIL,rotation=[0,45,0],pivot=[0,8.25,0]),
        cube([-0.28,8.35,-0.28],[0.56,1.05,0.56],LIGHT),
        cube([-5.35,7.86,-0.26],[10.7,0.22,0.52],DETAIL),
        cube([-0.26,7.86,-5.35],[0.52,0.22,10.7],DETAIL),
    ],pivot=[0,8.2,0],parent='root'),
    bone('ball',[
        cube([4.55,8.72,-0.4],[0.8,0.8,0.8],LIGHT),
    ],pivot=[0,9.0,0],parent='root'),
    bone('lights',[
        cube([-5.65,7.86,4.82],[11.3,0.18,0.28],LIGHT),
    ],pivot=[0,8.0,4.8],parent='root'),
])

# POKER ----------------------------------------------------------------------
# Oval-ish poker table built from a center slab plus short end wings, with five community cards and chip rack.
models['casino_poker']=geo('casino_poker',[
    bone('root',[
        cube([-4.0,0,-3.25],[8.0,6.8,6.5],BODY),
        cube([-4.8,0,-4.0],[9.6,0.9,8.0],DARK),
        cube([-5.45,6.65,-5.0],[10.9,1.0,10.0],SURFACE),
        cube([-7.1,6.82,-3.65],[1.85,0.82,7.3],SURFACE),
        cube([5.25,6.82,-3.65],[1.85,0.82,7.3],SURFACE),
        cube([-6.55,7.5,-4.2],[13.1,0.45,0.52],ACCENT),
        cube([-6.55,7.5,3.68],[13.1,0.45,0.52],ACCENT),
        cube([-6.95,7.5,-3.35],[0.52,0.45,6.7],ACCENT),
        cube([6.43,7.5,-3.35],[0.52,0.45,6.7],ACCENT),
        cube([-2.4,7.72,2.55],[4.8,0.35,1.35],DARK),
        cube([-2.0,7.9,2.75],[0.9,0.42,0.9],DETAIL),
        cube([-0.45,7.9,2.75],[0.9,0.42,0.9],ACCENT),
        cube([1.1,7.9,2.75],[0.9,0.42,0.9],LIGHT),
    ],pivot=[0,0,0]),
    bone('cards',[
        cube([-4.2,7.78,-1.45],[1.45,0.18,2.05],LIGHT,rotation=[0,-6,0],pivot=[-3.48,7.9,-0.42]),
        cube([-2.35,7.78,-1.58],[1.45,0.18,2.05],LIGHT,rotation=[0,-3,0],pivot=[-1.63,7.9,-0.55]),
        cube([-0.72,7.78,-1.62],[1.45,0.18,2.05],LIGHT),
        cube([0.90,7.78,-1.58],[1.45,0.18,2.05],LIGHT,rotation=[0,3,0],pivot=[1.62,7.9,-0.55]),
        cube([2.75,7.78,-1.45],[1.45,0.18,2.05],LIGHT,rotation=[0,6,0],pivot=[3.47,7.9,-0.42]),
    ],pivot=[0,7.9,-0.5],parent='root'),
    bone('lights',[
        cube([-4.85,7.88,4.28],[9.7,0.18,0.26],LIGHT),
    ],pivot=[0,8.0,4.25],parent='root'),
])

# BLACKJACK ------------------------------------------------------------------
# Dealer-oriented table: straight dealer rail at the back, angled player wings at the front,
# visible card shoe, dealer tray and three betting spots.
models['casino_blackjack']=geo('casino_blackjack',[
    bone('root',[
        cube([-3.9,0,-3.2],[7.8,6.8,6.4],BODY),
        cube([-4.7,0,-3.9],[9.4,0.9,7.8],DARK),
        cube([-5.5,6.65,-4.35],[11.0,1.0,8.7],SURFACE),
        cube([-7.0,6.8,-2.9],[2.0,0.85,6.0],SURFACE,rotation=[0,-12,0],pivot=[-5.7,7.2,0]),
        cube([5.0,6.8,-2.9],[2.0,0.85,6.0],SURFACE,rotation=[0,12,0],pivot=[5.7,7.2,0]),
        cube([-5.75,7.5,3.82],[11.5,0.48,0.52],ACCENT),
        cube([-6.55,7.5,-3.65],[5.2,0.48,0.52],ACCENT,rotation=[0,-10,0],pivot=[-4.2,7.72,-3.35]),
        cube([1.35,7.5,-3.65],[5.2,0.48,0.52],ACCENT,rotation=[0,10,0],pivot=[4.2,7.72,-3.35]),
        cube([3.55,7.72,1.65],[2.25,1.05,2.8],DARK),
        cube([3.82,8.02,1.95],[1.7,0.35,2.1],DETAIL),
        cube([-5.0,7.78,2.35],[2.2,0.42,1.25],DETAIL),
        cube([-4.55,7.82,-2.0],[1.55,0.18,1.1],ACCENT),
        cube([-0.78,7.82,-2.35],[1.55,0.18,1.1],ACCENT),
        cube([3.0,7.82,-2.0],[1.55,0.18,1.1],ACCENT),
    ],pivot=[0,0,0]),
    bone('cards',[
        cube([-1.75,7.82,-0.6],[1.5,0.18,2.15],LIGHT,rotation=[0,-8,0],pivot=[-1.0,7.92,0.45]),
        cube([0.35,7.82,-0.6],[1.5,0.18,2.15],LIGHT,rotation=[0,8,0],pivot=[1.10,7.92,0.45]),
    ],pivot=[0,7.95,0.4],parent='root'),
    bone('lights',[
        cube([-4.7,7.9,3.98],[9.4,0.18,0.26],LIGHT),
    ],pivot=[0,8.0,3.95],parent='root'),
])

# DICE / CRAPS ---------------------------------------------------------------
# A real craps-like recessed tray: long felt bed, raised rails and two large animated dice.
models['casino_dice']=geo('casino_dice',[
    bone('root',[
        cube([-4.0,0,-3.6],[8.0,6.4,7.2],BODY),
        cube([-4.8,0,-4.3],[9.6,0.9,8.6],DARK),
        cube([-7.1,6.45,-5.3],[14.2,0.85,10.6],DARK),
        cube([-6.55,6.75,-4.75],[13.1,0.42,9.5],SURFACE),
        cube([-7.25,7.15,-5.45],[14.5,1.2,0.72],ACCENT),
        cube([-7.25,7.15,4.73],[14.5,1.2,0.72],ACCENT),
        cube([-7.25,7.15,-4.75],[0.72,1.2,9.5],ACCENT),
        cube([6.53,7.15,-4.75],[0.72,1.2,9.5],ACCENT),
        cube([-5.6,7.22,-0.18],[11.2,0.18,0.36],DETAIL),
        cube([-0.18,7.22,-3.9],[0.36,0.18,7.8],DETAIL),
        cube([-5.45,7.23,3.72],[2.1,0.20,0.55],LIGHT),
        cube([3.35,7.23,-4.28],[2.1,0.20,0.55],LIGHT),
    ],pivot=[0,0,0]),
    bone('dice',[
        cube([-3.55,7.8,-1.75],[2.75,2.75,2.75],LIGHT,rotation=[10,17,-7],pivot=[-2.18,9.18,-0.38]),
        cube([1.0,7.95,0.45],[2.75,2.75,2.75],LIGHT,rotation=[-7,-20,10],pivot=[2.38,9.33,1.82]),
    ],pivot=[0,9.2,0.5],parent='root'),
    bone('lights',[
        cube([-5.2,8.18,4.86],[10.4,0.18,0.25],LIGHT),
    ],pivot=[0,8.3,4.85],parent='root'),
])

for name, data in models.items():
    write(f'src/main/resources/assets/emipokemon/geo/{name}.geo.json', json.dumps(data, indent=2, separators=(',', ': ')) + '\n')
