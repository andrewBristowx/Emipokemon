from pathlib import Path
import struct,zlib,random

root=Path('.')

def png(w,h,p):
    raw=b''.join(b'\x00'+bytes(p[y*w*4:(y+1)*w*4]) for y in range(h))
    def ch(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+ch(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+ch(b'IDAT',zlib.compress(raw,9))+ch(b'IEND',b'')

def add(c,d): return tuple(max(0,min(255,v+d)) for v in c)

def atlas(kind,accent):
    w=h=128; px=bytearray(w*h*4); rng=random.Random(3900+sum(map(ord,kind)))
    def setp(x,y,c):
        if 0<=x<w and 0<=y<h:
            i=(y*w+x)*4; px[i:i+4]=bytes((*c,255))
    def rect(x0,y0,x1,y1,c):
        for y in range(y0,y1):
            for x in range(x0,x1): setp(x,y,c)
    def outline(x0,y0,x1,y1,c,t=1):
        rect(x0,y0,x1,y0+t,c); rect(x0,y1-t,x1,y1,c); rect(x0,y0,x0+t,y1,c); rect(x1-t,y0,x1,y1,c)
    def speckle(x0,y0,x1,y1,base,amount=10):
        for y in range(y0,y1):
            for x in range(x0,x1): setp(x,y,add(base,rng.randint(-amount,amount)))

    # WOOD zone 0..45: original plank-like pixels, not copied from Minecraft assets.
    wood=(126,82,48); speckle(0,0,46,46,wood,9)
    for y in (8,18,28,38): rect(0,y,46,y+2,(88,57,37))
    for y0 in (0,10,20,30,40):
        off=0 if (y0//10)%2==0 else 12
        for x in range(off,46,23): rect(x,y0,x+2,min(46,y0+10),(96,61,38))
    for x,y in ((7,5),(31,14),(18,25),(39,34)): rect(x,y,x+3,y+2,(78,49,31))

    # METAL zone 48..85: copper/brass-like panels with rivets.
    metal=(173,112,68) if kind in ('slot','roulette') else (92,128,126)
    speckle(48,0,86,46,metal,7)
    for y in range(5,46,10): rect(49,y,85,y+1,add(metal,-24))
    for x in range(52,86,10): rect(x,1,x+1,45,add(metal,16))
    for x in (52,82):
        for y in (4,22,40): rect(x,y,x+2,y+2,(232,197,117))

    # FELT / emerald-like zone 0..45,48..87.
    felt=(35,111,69); speckle(0,48,46,88,felt,5)
    for y in range(51,88,6):
        for x in range((y%12)//2,46,8): setp(x,y,add(felt,10))
    outline(1,49,45,87,(216,171,69),2)

    # LIGHT / quartz-iron-like panel zone.
    light=(226,222,204); speckle(48,48,86,88,light,6)
    for y in (55,69,83): rect(49,y,85,y+1,(185,184,177))
    for x in (58,76): rect(x,49,x+1,87,(241,239,224))

    # STONE / deepslate-like but readable.
    stone=(70,75,83); speckle(88,0,127,46,stone,10)
    for y in range(4,46,8): rect(89,y,126,y+1,(47,52,59))
    for x in range(92,127,11): rect(x,1,x+1,45,(91,95,102))

    # DETAIL zone carries each machine's identity color and game motif.
    speckle(88,48,127,88,accent,5); outline(89,49,126,87,add(accent,-35),2)
    if kind=='slot':
        for x,label in ((92,0),(103,1),(114,2)):
            rect(x,54,x+9,80,(243,235,199)); outline(x,54,x+9,80,(91,54,69),1)
            if label==0: rect(x+2,61,x+6,65,(205,53,66)); rect(x+5,64,x+7,70,(205,53,66))
            elif label==1: rect(x+2,61,x+7,64,(45,47,52)); rect(x+2,67,x+7,70,(45,47,52))
            else: rect(x+2,60,x+6,64,(230,56,71)); rect(x+5,64,x+7,69,(61,137,78))
    elif kind=='chip_exchange':
        for yy in (55,65,75):
            for xx in (94,104,114):
                rect(xx,yy,xx+7,yy+5,(233,190,70)); outline(xx,yy,xx+7,yy+5,(121,79,31),1)
    elif kind=='ticket_exchange':
        for yy in (54,62,70):
            rect(94,yy,121,yy+6,(246,235,205)); outline(94,yy,121,yy+6,(188,84,111),1)
            for xx in range(98,119,5): rect(xx,yy+2,xx+2,yy+4,(73,141,172))
    elif kind=='roulette':
        for i,xx in enumerate(range(92,123,4)): rect(xx,55,xx+3,80,(183,48,51) if i%2==0 else (38,41,46))
        rect(92,66,123,69,(226,194,111)); rect(106,54,109,81,(226,194,111))
    elif kind=='poker':
        for xx in (92,99,106,113,120):
            rect(xx,57,xx+5,73,(247,240,218)); outline(xx,57,xx+5,73,(114,72,50),1)
    elif kind=='blackjack':
        rect(93,55,124,80,(40,105,66)); outline(93,55,124,80,(222,175,69),2)
        rect(98,60,106,75,(247,239,216)); rect(112,60,120,75,(247,239,216))
    elif kind=='dice':
        rect(94,55,106,67,(240,237,220)); rect(111,68,123,80,(240,237,220))
        for x,y in ((97,58),(103,64),(114,71),(120,77),(114,77),(120,71)): rect(x,y,x+2,y+2,(37,39,44))

    # Lower spare zone: branded planks/metal mix used by long cabinet pieces.
    speckle(0,90,127,127,(105,72,47),8)
    for y in (99,109,119): rect(0,y,127,y+2,(73,49,34))
    rect(3,93,124,96,add(accent,15)); rect(3,122,124,125,add(accent,-20))

    p=root/f'src/main/resources/assets/emipokemon/textures/block/casino_{kind}.png'; p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes(png(w,h,px))

accents={
 'slot':(147,68,116),'chip_exchange':(54,139,132),'ticket_exchange':(205,91,125),
 'roulette':(171,60,57),'poker':(126,70,48),'blackjack':(64,86,135),'dice':(132,68,91)
}
for k,a in accents.items(): atlas(k,a)
