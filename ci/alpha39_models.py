from pathlib import Path
import json

root=Path('.')

def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text)

def cube(origin,size,uv,rotation=None,pivot=None):
    d={'origin':origin,'size':size,'uv':uv}
    if rotation is not None: d['rotation']=rotation
    if pivot is not None: d['pivot']=pivot
    return d

def bone(name,cubes,pivot=None,parent=None):
    d={'name':name}
    if parent: d['parent']=parent
    if pivot is not None: d['pivot']=pivot
    if cubes: d['cubes']=cubes
    return d

def geo(identifier,bones,tall=False):
    return {'format_version':'1.12.0','minecraft:geometry':[{'description':{
        'identifier':'geometry.emipokemon.'+identifier,
        'texture_width':128,'texture_height':128,
        'visible_bounds_width':2.2 if not tall else 2.4,
        'visible_bounds_height':2.2 if not tall else 3.2,
        'visible_bounds_offset':[0,0.75,0] if not tall else [0,1.0,0]
    },'bones':bones}]}

# Atlas zones intentionally represent original block-like materials.
WOOD=[0,0]; METAL=[48,0]; FELT=[0,48]; LIGHT=[48,48]; STONE=[88,0]; DETAIL=[88,48]
models={}

# ~1.95 block high casino cabinet.
models['casino_slot']=geo('casino_slot',[
 bone('root',[
  cube([-6.6,0,-4.2],[13.2,1.2,8.4],STONE),
  cube([-6.1,1.2,-3.8],[12.2,7.8,7.6],WOOD),
  cube([-5.8,2.2,-4.45],[11.6,2.0,0.9],METAL),
  cube([-4.7,2.55,-5.0],[9.4,1.0,0.55],STONE),
  cube([-5.9,8.5,-4.55],[11.8,1.6,1.45],METAL),
  cube([-5.4,9.7,-3.65],[10.8,12.8,7.3],WOOD),
  cube([-5.15,12.1,-4.15],[10.3,8.6,0.65],STONE),
  cube([-5.6,22.0,-3.9],[11.2,4.7,7.8],METAL),
  cube([-6.3,26.3,-4.1],[12.6,4.8,8.2],WOOD),
  cube([-5.4,27.1,-4.7],[10.8,2.5,0.7],LIGHT),
  cube([-4.2,30.2,-4.35],[8.4,0.65,0.55],DETAIL),
 ],pivot=[0,0,0]),
 bone('reel1',[cube([-4.45,13.1,-4.8],[2.55,6.55,0.65],LIGHT),cube([-4.05,15.9,-5.12],[1.75,0.8,0.32],DETAIL)],pivot=[-3.18,16.35,-4.45],parent='root'),
 bone('reel2',[cube([-1.28,13.1,-4.8],[2.55,6.55,0.65],LIGHT),cube([-0.88,15.9,-5.12],[1.75,0.8,0.32],DETAIL)],pivot=[0,16.35,-4.45],parent='root'),
 bone('reel3',[cube([1.90,13.1,-4.8],[2.55,6.55,0.65],LIGHT),cube([2.30,15.9,-5.12],[1.75,0.8,0.32],DETAIL)],pivot=[3.18,16.35,-4.45],parent='root'),
 bone('lever',[cube([6.15,10.0,-0.5],[0.75,8.8,0.75],METAL),cube([5.75,18.1,-0.9],[1.55,1.55,1.55],DETAIL)],pivot=[6.5,10.2,-0.15],parent='root'),
 bone('lights',[cube([-5.65,29.75,-4.48],[11.3,0.45,0.4],LIGHT),cube([-5.45,21.65,-4.65],[10.9,0.35,0.35],LIGHT)],pivot=[0,29.8,-4.3],parent='root'),
],True)

models['casino_chip_exchange']=geo('casino_chip_exchange',[
 bone('root',[
  cube([-6.4,0,-4.1],[12.8,1.2,8.2],STONE),
  cube([-5.9,1.2,-3.7],[11.8,10.2,7.4],METAL),
  cube([-5.4,3.1,-4.5],[10.8,3.0,1.0],STONE),
  cube([-4.3,3.55,-5.0],[8.6,1.4,0.55],LIGHT),
  cube([-5.65,10.8,-4.35],[11.3,2.0,1.25],WOOD),
  cube([-5.55,12.2,-3.55],[11.1,10.7,7.1],METAL),
  cube([-4.8,15.8,-4.15],[9.6,5.2,0.7],STONE),
  cube([-4.3,16.5,-4.55],[8.6,3.7,0.35],LIGHT),
  cube([-6.2,22.4,-3.85],[12.4,4.2,7.7],WOOD),
  cube([-6.5,26.1,-4.05],[13.0,4.7,8.1],METAL),
  cube([-5.25,27.0,-4.7],[10.5,2.35,0.65],LIGHT),
 ],pivot=[0,0,0]),
 bone('spinner',[
  cube([-3.0,8.0,-5.1],[6.0,0.5,0.5],LIGHT),cube([-0.25,6.1,-5.1],[0.5,4.5,0.5],LIGHT),
  cube([-1.9,6.8,-5.45],[3.8,3.8,0.4],DETAIL),cube([-1.3,7.4,-5.7],[2.6,2.6,0.25],METAL,rotation=[0,45,0],pivot=[0,8.7,-5.55])
 ],pivot=[0,8.6,-5.25],parent='root'),
 bone('lights',[cube([-5.5,29.65,-4.4],[11.0,0.45,0.35],LIGHT),cube([-4.8,15.55,-4.4],[9.6,0.3,0.3],LIGHT)],pivot=[0,29.7,-4.2],parent='root'),
],True)

models['casino_ticket_exchange']=geo('casino_ticket_exchange',[
 bone('root',[
  cube([-6.3,0,-4.05],[12.6,1.2,8.1],STONE),cube([-5.75,1.2,-3.65],[11.5,10.4,7.3],WOOD),
  cube([-5.0,2.4,-4.45],[10.0,3.1,0.9],METAL),cube([-4.15,2.8,-4.95],[8.3,1.5,0.55],LIGHT),
  cube([-5.55,10.9,-4.25],[11.1,2.0,1.2],METAL),cube([-5.45,12.3,-3.55],[10.9,10.4,7.1],WOOD),
  cube([-4.75,16.0,-4.15],[9.5,4.8,0.65],STONE),cube([-4.2,16.6,-4.5],[8.4,3.55,0.35],LIGHT),
  cube([-6.15,22.2,-3.8],[12.3,4.0,7.6],METAL),cube([-6.45,25.8,-4.0],[12.9,4.9,8.0],WOOD),
  cube([-5.15,26.8,-4.65],[10.3,2.4,0.65],LIGHT),
 ],pivot=[0,0,0]),
 bone('cards',[
  cube([-3.0,10.1,-5.0],[6.0,1.2,0.25],LIGHT),cube([-2.8,9.0,-5.25],[5.6,1.15,0.2],LIGHT,rotation=[10,0,0],pivot=[0,10.1,-5.05]),
  cube([-2.55,7.9,-5.45],[5.1,1.15,0.18],DETAIL,rotation=[14,0,0],pivot=[0,9.0,-5.25]),cube([-2.3,6.8,-5.62],[4.6,1.1,0.16],LIGHT,rotation=[18,0,0],pivot=[0,7.9,-5.45])
 ],pivot=[0,10.5,-4.9],parent='root'),
 bone('lights',[cube([-5.35,29.5,-4.35],[10.7,0.45,0.35],LIGHT),cube([-5.0,10.65,-4.5],[10.0,0.3,0.3],LIGHT)],pivot=[0,29.6,-4.1],parent='root'),
],True)

# Furniture remains around one block high but gains heavier rails, layered tops and recognizable hardware.
models['casino_roulette']=geo('casino_roulette',[
 bone('root',[
  cube([-4.7,0,-4.7],[9.4,6.6,9.4],STONE),cube([-5.2,0,-5.2],[10.4,1.0,10.4],WOOD),
  cube([-7.4,6.4,-5.0],[14.8,1.1,10.0],WOOD),cube([-5.0,6.4,-7.4],[10.0,1.1,14.8],WOOD),
  cube([-7.65,7.25,-4.5],[15.3,0.65,9.0],METAL),cube([-4.5,7.25,-7.65],[9.0,0.65,15.3],METAL),
  cube([-5.7,7.5,-5.7],[11.4,0.55,11.4],STONE),
 ],pivot=[0,0,0]),
 bone('spinner',[cube([-5.1,8.0,-0.25],[10.2,0.35,0.5],DETAIL),cube([-0.25,8.0,-5.1],[0.5,0.35,10.2],LIGHT),cube([-4.65,8.0,-0.22],[9.3,0.32,0.44],LIGHT,rotation=[0,45,0],pivot=[0,8.15,0]),cube([-4.65,8.0,-0.22],[9.3,0.32,0.44],DETAIL,rotation=[0,-45,0],pivot=[0,8.15,0]),cube([-1.1,7.96,-1.1],[2.2,0.65,2.2],METAL,rotation=[0,45,0],pivot=[0,8.25,0]),cube([-0.3,8.45,-0.3],[0.6,1.25,0.6],LIGHT)],pivot=[0,8.2,0],parent='root'),
 bone('ball',[cube([4.9,8.8,-0.42],[0.84,0.84,0.84],LIGHT)],pivot=[0,9.1,0],parent='root'),bone('lights',[cube([-6.2,7.85,5.35],[12.4,0.2,0.3],LIGHT)],pivot=[0,8,5.3],parent='root')
])

models['casino_poker']=geo('casino_poker',[
 bone('root',[cube([-4.5,0,-3.6],[9,6.5,7.2],STONE),cube([-5.1,0,-4.2],[10.2,1.0,8.4],WOOD),cube([-6.0,6.35,-5.4],[12,1.15,10.8],WOOD),cube([-7.65,6.55,-3.9],[1.8,0.95,7.8],WOOD),cube([5.85,6.55,-3.9],[1.8,0.95,7.8],WOOD),cube([-7.5,7.35,-4.6],[15,0.6,9.2],METAL),cube([-6.9,7.55,-4.0],[13.8,0.38,8.0],FELT),cube([-2.6,7.95,2.6],[5.2,0.42,1.35],STONE)],pivot=[0,0,0]),
 bone('cards',[cube([-4.3,8.05,-1.45],[1.45,0.18,2.0],LIGHT,rotation=[0,-7,0],pivot=[-3.55,8.15,-.45]),cube([-2.38,8.05,-1.58],[1.45,0.18,2.0],LIGHT),cube([-0.72,8.05,-1.62],[1.45,0.18,2.0],LIGHT),cube([0.95,8.05,-1.58],[1.45,0.18,2.0],LIGHT),cube([2.86,8.05,-1.45],[1.45,0.18,2.0],LIGHT,rotation=[0,7,0],pivot=[3.58,8.15,-.45])],pivot=[0,8.1,-.5],parent='root'),
 bone('lights',[cube([-5.4,7.92,3.55],[10.8,0.2,0.28],LIGHT)],pivot=[0,8,3.5],parent='root')
])

models['casino_blackjack']=geo('casino_blackjack',[
 bone('root',[cube([-4.6,0,-3.8],[9.2,6.5,7.6],STONE),cube([-5.2,0,-4.35],[10.4,1.0,8.7],WOOD),cube([-5.9,6.35,-5.5],[11.8,1.1,11],WOOD),cube([-7.7,6.55,-3.8],[2.0,0.95,7.6],WOOD),cube([5.7,6.55,-3.8],[2.0,0.95,7.6],WOOD),cube([-7.45,7.32,-4.8],[14.9,0.6,9.6],METAL),cube([-6.9,7.5,-4.25],[13.8,0.4,8.5],FELT),cube([4.4,7.95,1.7],[2.1,1.3,3.0],STONE),cube([-6.0,7.95,2.5],[1.7,0.45,1.7],DETAIL)],pivot=[0,0,0]),
 bone('cards',[cube([-2.0,8.08,-1.35],[1.55,0.18,2.2],LIGHT,rotation=[0,-8,0],pivot=[-1.2,8.15,-.25]),cube([0.45,8.08,-1.35],[1.55,0.18,2.2],LIGHT,rotation=[0,8,0],pivot=[1.25,8.15,-.25])],pivot=[0,8.15,-.3],parent='root'),
 bone('lights',[cube([-5.5,7.92,3.7],[11,0.2,0.28],LIGHT)],pivot=[0,8,3.65],parent='root')
])

models['casino_dice']=geo('casino_dice',[
 bone('root',[cube([-4.6,0,-4.6],[9.2,6.4,9.2],STONE),cube([-5.2,0,-5.2],[10.4,1.0,10.4],WOOD),cube([-7.55,6.2,-7.55],[15.1,1.2,15.1],WOOD),cube([-6.9,7.2,-6.9],[13.8,0.4,13.8],FELT),cube([-7.75,7.25,-7.4],[15.5,0.85,0.65],METAL),cube([-7.75,7.25,6.75],[15.5,0.85,0.65],METAL),cube([-7.4,7.25,-6.75],[0.65,0.85,13.5],METAL),cube([6.75,7.25,-6.75],[0.65,0.85,13.5],METAL),cube([-5.6,7.72,-0.22],[11.2,0.24,0.44],DETAIL),cube([-0.22,7.72,-5.6],[0.44,0.24,11.2],DETAIL)],pivot=[0,0,0]),
 bone('dice',[cube([-4.1,8.5,-2.0],[3.2,3.2,3.2],LIGHT,rotation=[9,16,-7],pivot=[-2.5,10.1,-.4]),cube([1.1,8.7,0.4],[3.2,3.2,3.2],LIGHT,rotation=[-7,-18,10],pivot=[2.7,10.3,2.0])],pivot=[0,10.2,0],parent='root'),
 bone('lights',[cube([-5.7,8.02,5.95],[11.4,0.22,0.3],LIGHT)],pivot=[0,8.1,5.9],parent='root')
])

for name,data in models.items():
    write(f'src/main/resources/assets/emipokemon/geo/{name}.geo.json',json.dumps(data,indent=2)+'\n')
