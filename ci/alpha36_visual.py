from pathlib import Path
import json, struct, zlib

root=Path('.')

def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text)

def cube(origin,size,uv,rotation=None,pivot=None):
    value={'origin':origin,'size':size,'uv':uv}
    if rotation is not None: value['rotation']=rotation
    if pivot is not None: value['pivot']=pivot
    return value

def bone(name,cubes,pivot=None,parent=None):
    value={'name':name}
    if parent: value['parent']=parent
    if pivot is not None: value['pivot']=pivot
    if cubes: value['cubes']=cubes
    return value

def geo(identifier,bones):
    return {
      'format_version':'1.12.0',
      'minecraft:geometry':[{
        'description':{
          'identifier':'geometry.emipokemon.'+identifier,
          'texture_width':128,'texture_height':128,
          'visible_bounds_width':2.0,'visible_bounds_height':2.0,
          'visible_bounds_offset':[0,0.75,0]
        },
        'bones':bones
      }]
    }

BODY=[0,0]; ACCENT=[48,0]; SURFACE=[0,48]; LIGHT=[48,48]; DARK=[88,0]; DETAIL=[88,48]

models={}
models['casino_slot']=geo('casino_slot',[
 bone('root',[
  cube([-6.0,0,-4.25],[12,13.5,8.5],BODY),
  cube([-6.25,0,-4.5],[12.5,1.25,9],DARK),
  cube([-6.5,13,-4.5],[13,2.25,9],ACCENT),
  cube([-5.75,6.4,-4.8],[11.5,6.5,0.65],ACCENT),
  cube([-5.15,7.0,-5.25],[10.3,5.15,0.55],SURFACE),
  cube([-3.7,3.1,-5.05],[7.4,1.15,0.8],DARK),
  cube([-2.8,4.65,-5.0],[5.6,0.65,0.7],LIGHT),
  cube([-5.2,1.7,-4.75],[2.0,3.6,0.65],ACCENT),
  cube([3.2,1.7,-4.75],[2.0,3.6,0.65],ACCENT),
 ],pivot=[0,0,0]),
 bone('reel1',[cube([-4.55,7.5,-5.72],[2.75,4.15,0.65],LIGHT)],pivot=[-3.18,9.58,-5.4],parent='root'),
 bone('reel2',[cube([-1.38,7.5,-5.72],[2.75,4.15,0.65],LIGHT)],pivot=[0,9.58,-5.4],parent='root'),
 bone('reel3',[cube([1.80,7.5,-5.72],[2.75,4.15,0.65],LIGHT)],pivot=[3.18,9.58,-5.4],parent='root'),
 bone('lever',[
  cube([6.0,5.8,-0.55],[0.85,4.6,0.85],DARK),
  cube([5.75,9.95,-0.85],[1.35,1.35,1.35],ACCENT),
 ],pivot=[6.42,6.0,-0.12],parent='root'),
 bone('lights',[cube([-5.45,14.05,-4.86],[10.9,0.62,0.45],LIGHT)],pivot=[0,14.35,-4.6],parent='root'),
])

models['casino_chip_exchange']=geo('casino_chip_exchange',[
 bone('root',[
  cube([-5.75,0,-4.2],[11.5,14.2,8.4],BODY),
  cube([-6.1,0,-4.45],[12.2,1.15,8.9],DARK),
  cube([-6.25,12.7,-4.45],[12.5,2.35,8.9],ACCENT),
  cube([-4.75,8.15,-4.7],[9.5,3.35,0.65],SURFACE),
  cube([-3.4,3.0,-4.95],[6.8,1.2,0.85],DARK),
  cube([-2.2,5.15,-4.9],[4.4,1.0,0.75],LIGHT),
  cube([-4.85,6.65,-4.75],[1.15,0.9,0.7],ACCENT),
  cube([3.7,6.65,-4.75],[1.15,0.9,0.7],ACCENT),
 ],pivot=[0,0,0]),
 bone('spinner',[
  cube([-2.6,6.25,-5.28],[5.2,0.45,0.45],LIGHT),
  cube([-0.22,4.05,-5.28],[0.45,4.85,0.45],LIGHT),
  cube([-1.05,5.2,-5.52],[2.1,2.1,0.35],ACCENT),
 ],pivot=[0,6.3,-5.1],parent='root'),
 bone('lights',[cube([-5.1,13.65,-4.78],[10.2,0.58,0.45],LIGHT)],pivot=[0,13.9,-4.5],parent='root'),
])

models['casino_ticket_exchange']=geo('casino_ticket_exchange',[
 bone('root',[
  cube([-5.65,0,-4.15],[11.3,14.35,8.3],BODY),
  cube([-6.05,0,-4.4],[12.1,1.15,8.8],DARK),
  cube([-6.15,12.85,-4.45],[12.3,2.1,8.9],ACCENT),
  cube([-4.65,8.55,-4.72],[9.3,3.05,0.65],SURFACE),
  cube([-3.75,4.0,-4.88],[7.5,1.0,0.78],DARK),
  cube([-3.15,5.55,-4.8],[6.3,0.72,0.7],LIGHT),
  cube([-4.9,2.0,-4.68],[1.3,4.6,0.55],ACCENT),
  cube([3.6,2.0,-4.68],[1.3,4.6,0.55],ACCENT),
 ],pivot=[0,0,0]),
 bone('cards',[
  cube([-2.45,4.35,-5.58],[4.9,2.5,0.28],LIGHT),
  cube([-2.1,4.7,-5.76],[4.2,1.8,0.18],ACCENT),
 ],pivot=[0,4.4,-5.5],parent='root'),
 bone('lights',[cube([-5.0,13.75,-4.82],[10.0,0.55,0.45],LIGHT)],pivot=[0,14,-4.5],parent='root'),
])

models['casino_roulette']=geo('casino_roulette',[
 bone('root',[
  cube([-4.5,0,-4.5],[9,7.25,9],BODY),
  cube([-5.1,0,-5.1],[10.2,1.0,10.2],DARK),
  cube([-7.25,7.0,-7.25],[14.5,1.45,14.5],SURFACE),
  cube([-7.25,8.35,-7.25],[14.5,0.55,0.75],ACCENT),
  cube([-7.25,8.35,6.5],[14.5,0.55,0.75],ACCENT),
  cube([-7.25,8.35,-6.5],[0.75,0.55,13.0],ACCENT),
  cube([6.5,8.35,-6.5],[0.75,0.55,13.0],ACCENT),
 ],pivot=[0,0,0]),
 bone('spinner',[
  cube([-5.65,8.62,-5.65],[11.3,0.62,11.3],DARK),
  cube([-6.0,9.18,-0.28],[12.0,0.35,0.56],LIGHT),
  cube([-0.28,9.18,-6.0],[0.56,0.35,12.0],ACCENT),
  cube([-5.4,9.18,-0.25],[10.8,0.3,0.5],ACCENT,rotation=[0,45,0],pivot=[0,9.3,0]),
  cube([-5.4,9.18,-0.25],[10.8,0.3,0.5],LIGHT,rotation=[0,-45,0],pivot=[0,9.3,0]),
 ],pivot=[0,9.25,0],parent='root'),
 bone('ball',[cube([4.85,9.65,-0.42],[0.84,0.84,0.84],LIGHT)],pivot=[0,10.0,0],parent='root'),
 bone('lights',[cube([-6.3,8.82,-6.3],[12.6,0.18,0.3],LIGHT)],pivot=[0,8.9,0],parent='root'),
])

models['casino_poker']=geo('casino_poker',[
 bone('root',[
  cube([-4.25,0,-3.5],[8.5,7.2,7],BODY),
  cube([-5.0,0,-4.2],[10,0.9,8.4],DARK),
  cube([-6.8,7.0,-5.6],[13.6,1.35,11.2],SURFACE),
  cube([-7.25,8.15,-5.25],[14.5,0.58,0.72],ACCENT),
  cube([-7.25,8.15,4.55],[14.5,0.58,0.72],ACCENT),
  cube([-7.1,8.15,-4.55],[0.72,0.58,9.1],ACCENT),
  cube([6.38,8.15,-4.55],[0.72,0.58,9.1],ACCENT),
  cube([-1.0,8.4,3.0],[2.0,0.42,1.4],DETAIL),
 ],pivot=[0,0,0]),
 bone('cards',[
  cube([-4.2,8.5,-1.5],[1.45,0.18,2.1],LIGHT,rotation=[0,-8,0],pivot=[-3.45,8.6,-0.45]),
  cube([-2.3,8.5,-1.7],[1.45,0.18,2.1],LIGHT,rotation=[0,-4,0],pivot=[-1.55,8.6,-0.65]),
  cube([-0.72,8.5,-1.75],[1.45,0.18,2.1],LIGHT),
  cube([0.9,8.5,-1.7],[1.45,0.18,2.1],LIGHT,rotation=[0,4,0],pivot=[1.65,8.6,-0.65]),
  cube([2.75,8.5,-1.5],[1.45,0.18,2.1],LIGHT,rotation=[0,8,0],pivot=[3.5,8.6,-0.45]),
 ],pivot=[0,8.5,-0.7],parent='root'),
 bone('lights',[cube([-5.2,8.54,4.0],[10.4,0.22,0.3],LIGHT)],pivot=[0,8.6,4.0],parent='root'),
])

models['casino_blackjack']=geo('casino_blackjack',[
 bone('root',[
  cube([-4.35,0,-3.7],[8.7,7.2,7.4],BODY),
  cube([-5.1,0,-4.35],[10.2,0.9,8.7],DARK),
  cube([-7.0,7.0,-5.7],[14.0,1.35,11.4],SURFACE),
  cube([-7.35,8.15,-5.45],[14.7,0.58,0.72],ACCENT),
  cube([-7.35,8.15,4.72],[14.7,0.58,0.72],ACCENT),
  cube([-7.15,8.15,-4.72],[0.72,0.58,9.44],ACCENT),
  cube([6.43,8.15,-4.72],[0.72,0.58,9.44],ACCENT),
  cube([4.15,8.4,1.85],[2.15,1.15,2.9],DARK),
  cube([-5.9,8.42,2.45],[1.7,0.45,1.7],DETAIL),
 ],pivot=[0,0,0]),
 bone('cards',[
  cube([-1.9,8.52,-1.4],[1.55,0.18,2.25],LIGHT,rotation=[0,-7,0],pivot=[-1.1,8.6,-0.3]),
  cube([0.35,8.52,-1.4],[1.55,0.18,2.25],LIGHT,rotation=[0,7,0],pivot=[1.15,8.6,-0.3]),
 ],pivot=[0,8.6,-0.3],parent='root'),
 bone('lights',[cube([-5.4,8.54,4.15],[10.8,0.22,0.3],LIGHT)],pivot=[0,8.6,4.15],parent='root'),
])

models['casino_dice']=geo('casino_dice',[
 bone('root',[
  cube([-4.2,0,-4.2],[8.4,7.2,8.4],BODY),
  cube([-4.95,0,-4.95],[9.9,0.9,9.9],DARK),
  cube([-7.0,7.0,-7.0],[14,1.35,14],SURFACE),
  cube([-7.3,8.12,-7.0],[14.6,0.58,0.72],ACCENT),
  cube([-7.3,8.12,6.28],[14.6,0.58,0.72],ACCENT),
  cube([-7.0,8.12,-6.28],[0.72,0.58,12.56],ACCENT),
  cube([6.28,8.12,-6.28],[0.72,0.58,12.56],ACCENT),
 ],pivot=[0,0,0]),
 bone('dice',[
  cube([-4.0,9.0,-1.9],[3.0,3.0,3.0],LIGHT,rotation=[8,15,-5],pivot=[-2.5,10.5,-0.4]),
  cube([1.15,9.15,0.25],[3.0,3.0,3.0],LIGHT,rotation=[-6,-18,9],pivot=[2.65,10.65,1.75]),
 ],pivot=[0,10.5,0],parent='root'),
 bone('lights',[cube([-5.5,8.5,5.85],[11,0.22,0.32],LIGHT)],pivot=[0,8.6,5.8],parent='root'),
])

for name,data in models.items():
    write(f'src/main/resources/assets/emipokemon/geo/{name}.geo.json', json.dumps(data,indent=2,separators=(',',': '))+'\n')

# Smoother, restrained shared animations. The bones are present only where relevant.
anim={
 'format_version':'1.8.0',
 'animations':{
  'animation.casino.idle':{
   'loop':True,'animation_length':3.0,
   'bones':{
    'lights':{'scale':{'0.0':[1,1,1],'1.5':[1.025,1.025,1.025],'3.0':[1,1,1]}},
    'spinner':{'rotation':{'0.0':[0,0,0],'3.0':[0,12,0]}}
   }
  },
  'animation.casino.play':{
   'loop':False,'animation_length':1.8,
   'bones':{
    'reel1':{'rotation':{'0.0':[0,0,0],'1.5':[720,0,0],'1.8':[720,0,0]}},
    'reel2':{'rotation':{'0.0':[0,0,0],'1.65':[900,0,0],'1.8':[900,0,0]}},
    'reel3':{'rotation':{'0.0':[0,0,0],'1.8':[1080,0,0]}},
    'lever':{'rotation':{'0.0':[0,0,0],'.3':[0,0,-36],'.8':[0,0,4],'1.2':[0,0,0]}},
    'spinner':{'rotation':{'0.0':[0,0,0],'1.8':[0,1080,0]}},
    'ball':{'rotation':{'0.0':[0,0,0],'1.8':[0,-1440,0]}},
    'cards':{'position':{'0.0':[0,0,0],'.35':[0,1.4,0],'.8':[0,.25,0],'1.2':[0,0,0]},'rotation':{'0.0':[0,0,0],'.35':[0,9,0],'.8':[0,-3,0],'1.2':[0,0,0]}},
    'dice':{'rotation':{'0.0':[0,0,0],'.55':[170,230,110],'1.15':[350,520,300],'1.8':[360,720,360]}},
    'lights':{'scale':{'0.0':[1,1,1],'.25':[1.08,1.08,1.08],'.55':[1,1,1],'.85':[1.06,1.06,1.06],'1.2':[1,1,1]}}
   }
  }
 }
}
write('src/main/resources/assets/emipokemon/animations/casino.animation.json',json.dumps(anim,indent=2)+'\n')

# Tiny dependency-free PNG writer for coherent original pixel atlases.
def png(width,height,pixels):
    raw=b''.join(b'\x00'+bytes(pixels[y*width:(y+1)*width]) for y in range(height))
    def chunk(t,d):
        return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',width,height,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')

def atlas(path,palette,kind):
    w=h=128
    px=bytearray(w*h*4)
    def rect(x0,y0,x1,y1,c):
        for y in range(max(0,y0),min(h,y1)):
            for x in range(max(0,x0),min(w,x1)):
                i=(y*w+x)*4; px[i:i+4]=bytes((*c,255))
    bg,accent,surface,light,dark,detail=palette
    rect(0,0,w,h,dark)
    rect(0,0,46,46,bg); rect(48,0,86,46,accent); rect(0,48,46,88,surface)
    rect(48,48,86,88,light); rect(88,0,127,46,dark); rect(88,48,127,88,detail)
    rect(0,90,127,127,bg)
    # clean casino trim and small pixel motifs
    for y in (2,42,92,123): rect(0,y,127,y+2,accent)
    for x in range(4,44,8): rect(x,4,x+3,7,light)
    for x in range(52,84,8): rect(x,4,x+4,8,light)
    # surface grid/felt detailing
    for x in range(3,45,7): rect(x,50,x+1,87,tuple(max(0,c-18) for c in surface))
    for y in range(52,88,7): rect(2,y,45,y+1,tuple(max(0,c-12) for c in surface))
    # highlight region gets stylized card/coin/dice marks
    if kind=='slot':
        rect(53,57,61,65,(220,40,80)); rect(64,56,72,66,(250,195,45)); rect(75,57,83,65,(95,205,120))
    elif kind=='chip_exchange':
        for r,c in [(0,(255,204,70)),(4,(235,90,165)),(8,(255,235,170))]: rect(58+r,58+r//2,76-r//2,61+r//2,c)
    elif kind=='ticket_exchange':
        rect(54,57,81,77,(255,240,205)); rect(57,61,78,63,accent); rect(57,68,78,70,accent)
    elif kind=='roulette':
        rect(53,54,82,82,(20,105,65)); rect(61,54,65,82,(210,45,55)); rect(70,54,74,82,(25,25,30))
    elif kind in ('poker','blackjack'):
        rect(56,54,78,82,(248,241,222)); rect(59,57,75,60,(205,50,80)); rect(59,64,75,67,(40,55,60))
    elif kind=='dice':
        rect(55,55,69,69,(248,245,235)); rect(71,69,84,82,(248,245,235));
        for x,y in [(59,59),(65,65),(75,73),(80,78)]: rect(x,y,x+2,y+2,(35,28,45))
    p=root/path; p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes(png(w,h,px))

palettes={
 'slot':((76,28,89),(235,77,159),(241,219,183),(255,204,84),(31,16,40),(90,205,196)),
 'chip_exchange':((62,31,83),(245,181,57),(25,104,87),(255,229,150),(25,18,35),(221,74,151)),
 'ticket_exchange':((117,41,102),(247,111,177),(246,222,187),(255,190,73),(38,18,43),(95,193,218)),
 'roulette':((69,33,42),(219,169,65),(24,103,65),(245,221,154),(28,20,27),(177,42,56)),
 'poker':((58,29,38),(218,171,70),(22,99,67),(245,235,204),(27,20,26),(208,75,139)),
 'blackjack':((61,30,42),(226,111,154),(24,105,69),(247,238,207),(26,20,28),(221,178,67)),
 'dice':((62,31,82),(229,173,63),(25,107,74),(246,242,225),(28,20,38),(220,80,153)),
}
for kind,pal in palettes.items():
    atlas(f'src/main/resources/assets/emipokemon/textures/block/casino_{kind}.png',pal,kind)
