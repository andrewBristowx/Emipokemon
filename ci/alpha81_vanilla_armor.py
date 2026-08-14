#!/usr/bin/env python3
"""Generate Emi armor from Mojang's official Minecraft 1.21.1 client JAR.

The client JAR is selected from Mojang's version manifest and verified against the
published SHA-1 before any texture is read. We then recolor the exact diamond armor
UVs and item icons while preserving their alpha and shading.
"""
from __future__ import annotations
import hashlib, io, json, struct, urllib.request, zipfile, zlib
from pathlib import Path

VERSION = "1.21.1"
MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/emipokemon/textures"
ASSETS = {
    "assets/minecraft/textures/models/armor/diamond_layer_1.png": OUT / "models/armor/emi_layer_1.png",
    "assets/minecraft/textures/models/armor/diamond_layer_2.png": OUT / "models/armor/emi_layer_2.png",
    "assets/minecraft/textures/item/diamond_helmet.png": OUT / "item/emi_helmet.png",
    "assets/minecraft/textures/item/diamond_chestplate.png": OUT / "item/emi_chestplate.png",
    "assets/minecraft/textures/item/diamond_leggings.png": OUT / "item/emi_leggings.png",
    "assets/minecraft/textures/item/diamond_boots.png": OUT / "item/emi_boots.png",
}
PALETTE=[(0.00,(37,9,34)),(0.20,(72,18,62)),(0.40,(128,31,99)),(0.60,(190,61,143)),(0.78,(232,113,187)),(0.92,(249,183,226)),(1.00,(255,224,244))]

def get_bytes(url:str)->bytes:
    with urllib.request.urlopen(url,timeout=90) as r:return r.read()
def get_json(url:str):return json.loads(get_bytes(url))
def official_client_jar()->bytes:
    manifest=get_json(MANIFEST)
    entry=next(v for v in manifest["versions"] if v["id"]==VERSION)
    meta=get_json(entry["url"])
    client=meta["downloads"]["client"]
    raw=get_bytes(client["url"])
    if hashlib.sha1(raw).hexdigest()!=client["sha1"]:raise SystemExit("official Minecraft client JAR SHA-1 mismatch")
    print(f"verified official Minecraft {VERSION} client JAR sha1={client['sha1']}")
    return raw

def paeth(a,b,c):
    p=a+b-c;pa=abs(p-a);pb=abs(p-b);pc=abs(p-c);return a if pa<=pb and pa<=pc else b if pb<=pc else c
def decode_png(raw:bytes):
    if raw[:8]!=b"\x89PNG\r\n\x1a\n":raise ValueError("not png")
    pos=8;width=height=ctype=None;idat=b""
    while pos<len(raw):
        n=struct.unpack(">I",raw[pos:pos+4])[0];typ=raw[pos+4:pos+8];dat=raw[pos+8:pos+8+n];pos+=12+n
        if typ==b"IHDR":
            width,height,depth,ctype,_,_,interlace=struct.unpack(">IIBBBBB",dat)
            if depth!=8 or interlace!=0 or ctype not in (2,6):raise ValueError(f"unsupported PNG depth={depth} ctype={ctype}")
        elif typ==b"IDAT":idat+=dat
        elif typ==b"IEND":break
    bpp=4 if ctype==6 else 3;data=zlib.decompress(idat);stride=width*bpp;prev=bytearray(stride);out=[];off=0
    for _ in range(height):
        ft=data[off];off+=1;src=bytearray(data[off:off+stride]);off+=stride;row=bytearray(stride)
        for i,x in enumerate(src):
            a=row[i-bpp] if i>=bpp else 0;b=prev[i];c=prev[i-bpp] if i>=bpp else 0
            if ft==0:v=x
            elif ft==1:v=(x+a)&255
            elif ft==2:v=(x+b)&255
            elif ft==3:v=(x+((a+b)//2))&255
            elif ft==4:v=(x+paeth(a,b,c))&255
            else:raise ValueError(f"bad filter {ft}")
            row[i]=v
        rgba=[]
        for i in range(0,stride,bpp):rgba.extend(row[i:i+4] if bpp==4 else (*row[i:i+3],255))
        out.append(bytearray(rgba));prev=row
    return width,height,out
def chunk(typ,data):return struct.pack(">I",len(data))+typ+data+struct.pack(">I",zlib.crc32(typ+data)&0xffffffff)
def encode_png(w,h,rows):
    payload=b"".join(b"\x00"+bytes(r) for r in rows);ihdr=struct.pack(">IIBBBBB",w,h,8,6,0,0,0)
    return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",ihdr)+chunk(b"IDAT",zlib.compress(payload,9))+chunk(b"IEND",b"")
def gradient(t):
    t=max(0.0,min(1.0,t))
    for (a,ca),(b,cb) in zip(PALETTE,PALETTE[1:]):
        if t<=b:
            u=(t-a)/(b-a) if b>a else 0;return tuple(round(ca[i]+(cb[i]-ca[i])*u) for i in range(3))
    return PALETTE[-1][1]
def recolor(raw):
    w,h,rows=decode_png(raw)
    for row in rows:
        for i in range(0,len(row),4):
            r,g,b,a=row[i:i+4]
            if a==0:continue
            rr,gg,bb=gradient((0.2126*r+0.7152*g+0.0722*b)/255.0);row[i:i+4]=bytes((rr,gg,bb,a))
    return encode_png(w,h,rows)
def main():
    jar=official_client_jar()
    with zipfile.ZipFile(io.BytesIO(jar)) as z:
        names=set(z.namelist())
        missing=[k for k in ASSETS if k not in names]
        if missing:
            candidates=[n for n in names if "diamond" in n and ("armor" in n or "equipment" in n or "/item/diamond_" in n)]
            raise SystemExit("required vanilla diamond assets missing: "+str(missing)+"; candidates="+str(sorted(candidates)[:100]))
        for key,out in ASSETS.items():
            raw=z.read(key);out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(recolor(raw));print(f"generated {out.relative_to(ROOT)} from verified client JAR {key}")
if __name__=="__main__":main()
