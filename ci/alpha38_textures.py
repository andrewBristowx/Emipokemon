from pathlib import Path
import struct, zlib

root=Path('.')

def png(width,height,pixels):
    raw=b''.join(b'\x00'+bytes(pixels[y*width*4:(y+1)*width*4]) for y in range(height))
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
    def outline(x0,y0,x1,y1,c,t=2):
        rect(x0,y0,x1,y0+t,c); rect(x0,y1-t,x1,y1,c); rect(x0,y0,x0+t,y1,c); rect(x1-t,y0,x1,y1,c)
    def pip(cx,cy,c): rect(cx-2,cy-2,cx+2,cy+2,c)
    body,accent,surface,light,dark,detail=palette
    rect(0,0,46,46,body); rect(48,0,86,46,accent); rect(0,48,46,88,surface)
    rect(48,48,86,88,light); rect(88,0,127,46,dark); rect(88,48,127,88,detail); rect(0,90,127,127,body)
    # subtle seams
    for y in range(7,46,9): rect(1,y,45,y+1,tuple(max(0,v-12) for v in body))
    for y in range(7,46,10): rect(49,y,85,y+1,tuple(max(0,v-14) for v in accent))

    if kind=='slot':
        # three reel lanes with classic 7 / BAR / cherry-like marks
        rect(49,49,85,87,(249,239,201));
        for x in (51,63,75): outline(x,52,x+9,83,(110,72,82),1)
        rect(53,57,58,59,(205,44,70)); rect(56,59,59,66,(205,44,70))
        rect(65,60,72,64,(49,49,55)); rect(65,66,72,69,(49,49,55)); rect(65,72,72,75,(49,49,55))
        rect(77,57,82,60,(227,50,68)); rect(79,60,82,67,(227,50,68)); rect(76,66,80,70,(66,154,88))
        rect(91,52,124,84,(78,50,74)); outline(94,55,121,81,(245,192,62),2)
    elif kind=='chip_exchange':
        rect(2,50,44,86,(220,236,220)); outline(5,53,41,83,(45,105,102),2)
        for y in (58,66,74):
            for x in range(9,37,8): outline(x,y,x+6,y+6,(236,185,62),1)
        rect(50,50,84,86,(250,237,181));
        for x in range(52,81,7): outline(x,58,x+6,68,(44,122,116),1)
        for x in range(55,78,7): outline(x,70,x+6,80,(184,77,126),1)
    elif kind=='ticket_exchange':
        rect(2,50,44,86,(244,214,201)); outline(6,54,40,82,(220,90,126),2)
        rect(10,60,36,64,(250,238,210)); rect(10,70,30,73,(78,153,188))
        for x in range(12,36,6): rect(x,77,x+3,79,(112,62,76))
        rect(50,50,84,86,(252,233,177)); outline(54,55,80,81,(221,91,127),2)
        for x in range(57,78,5): rect(x,67,x+2,69,(90,90,100))
    elif kind=='roulette':
        rect(2,50,44,86,(31,116,71));
        # alternating wheel sectors impression
        for i,x in enumerate(range(4,43,5)): rect(x,53,x+3,82,(196,53,57) if i%2==0 else (35,35,40))
        rect(20,51,24,85,(240,220,170)); rect(2,66,44,70,(240,220,170))
        rect(50,50,84,86,(245,230,190)); outline(54,54,80,82,(133,84,54),2)
    elif kind=='poker':
        rect(2,50,44,86,(30,112,67));
        for x in (5,13,21,29,37):
            rect(x,59,x+6,76,(247,239,216)); outline(x,59,x+6,76,(116,77,55),1)
        for x,c in ((52,(214,169,68)),(61,(143,57,73)),(70,(245,232,199))):
            outline(x,58,x+7,65,c,2); outline(x,66,x+7,73,c,2)
    elif kind=='blackjack':
        rect(2,50,44,86,(31,114,70));
        rect(4,55,42,58,(220,174,69));
        for x in (6,19,32): outline(x,67,x+8,79,(245,231,200),1)
        rect(50,50,84,86,(248,237,207)); outline(54,55,80,81,(57,78,118),2)
        rect(58,60,65,76,(176,63,75)); rect(69,60,76,76,(42,52,78))
    elif kind=='dice':
        rect(2,50,44,86,(37,112,75)); outline(5,53,41,83,(222,176,68),2)
        rect(11,58,23,70,(248,243,226)); rect(25,68,37,80,(248,243,226))
        for a,b in ((14,61),(20,67),(28,71),(34,77),(28,77),(34,71)): pip(a,b,(44,38,47))
        rect(50,50,84,86,(247,239,218)); outline(54,54,80,82,(132,61,82),2)

    p=root/path; p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes(png(w,h,px))

palettes={
 'slot':((142,58,112),(235,174,55),(91,54,128),(252,235,170),(79,43,77),(56,171,168)),
 'chip_exchange':((45,122,116),(232,181,70),(218,233,208),(250,238,182),(43,76,78),(186,83,132)),
 'ticket_exchange':((217,176,154),(222,91,127),(244,213,199),(252,232,169),(112,62,76),(76,157,190)),
 'roulette':((133,84,54),(225,177,68),(32,116,72),(245,229,190),(73,48,38),(190,57,58)),
 'poker':((119,75,50),(214,169,68),(30,112,67),(245,232,199),(70,48,39),(143,57,73)),
 'blackjack':((57,78,118),(220,174,69),(31,114,70),(248,237,207),(42,52,78),(176,63,75)),
 'dice':((132,61,82),(222,176,68),(37,112,75),(247,239,218),(75,48,61),(107,83,147)),
}
for kind,pal in palettes.items(): atlas(f'src/main/resources/assets/emipokemon/textures/block/casino_{kind}.png',pal,kind)
