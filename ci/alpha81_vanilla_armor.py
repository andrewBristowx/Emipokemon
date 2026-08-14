#!/usr/bin/env python3
"""Generate Emi armor from Minecraft 1.21.1's official diamond armor/item textures.

The script downloads Mojang's official 1.21.1 asset index, verifies each object by SHA-1,
then recolors the exact vanilla diamond UVs into Emi's pink/magenta palette while preserving
all transparency and shading. This avoids hand-drawn UV mistakes that made alpha.79/.80 look
like rectangular clothing instead of real Minecraft armor.
"""
from __future__ import annotations
import hashlib, json, struct, urllib.request, zlib
from pathlib import Path

VERSION = "1.21.1"
MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
ASSET_ROOT = "https://resources.download.minecraft.net"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/emipokemon/textures"

ASSETS = {
    "minecraft/textures/models/armor/diamond_layer_1.png": OUT / "models/armor/emi_layer_1.png",
    "minecraft/textures/models/armor/diamond_layer_2.png": OUT / "models/armor/emi_layer_2.png",
    "minecraft/textures/item/diamond_helmet.png": OUT / "item/emi_helmet.png",
    "minecraft/textures/item/diamond_chestplate.png": OUT / "item/emi_chestplate.png",
    "minecraft/textures/item/diamond_leggings.png": OUT / "item/emi_leggings.png",
    "minecraft/textures/item/diamond_boots.png": OUT / "item/emi_boots.png",
}

PALETTE = [
    (0.00, (37, 9, 34)),
    (0.20, (72, 18, 62)),
    (0.40, (128, 31, 99)),
    (0.60, (190, 61, 143)),
    (0.78, (232, 113, 187)),
    (0.92, (249, 183, 226)),
    (1.00, (255, 224, 244)),
]

def get_json(url: str):
    with urllib.request.urlopen(url, timeout=60) as r:
        return json.load(r)

def get_bytes(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=60) as r:
        return r.read()

def fetch_asset_index():
    manifest = get_json(MANIFEST)
    entry = next(v for v in manifest["versions"] if v["id"] == VERSION)
    version_meta = get_json(entry["url"])
    index_meta = version_meta["assetIndex"]
    raw = get_bytes(index_meta["url"])
    if hashlib.sha1(raw).hexdigest() != index_meta["sha1"]:
        raise SystemExit("Mojang asset index SHA-1 mismatch")
    return json.loads(raw)

def fetch_object(index, key: str) -> bytes:
    obj = index["objects"][key]
    sha1 = obj["hash"]
    raw = get_bytes(f"{ASSET_ROOT}/{sha1[:2]}/{sha1}")
    if hashlib.sha1(raw).hexdigest() != sha1:
        raise SystemExit(f"SHA-1 mismatch for {key}")
    return raw

def paeth(a,b,c):
    p=a+b-c; pa=abs(p-a); pb=abs(p-b); pc=abs(p-c)
    return a if pa<=pb and pa<=pc else b if pb<=pc else c

def decode_png(raw: bytes):
    if raw[:8] != b"\x89PNG\r\n\x1a\n": raise ValueError("not png")
    pos=8; width=height=ctype=None; idat=b""
    while pos < len(raw):
        n=struct.unpack(">I",raw[pos:pos+4])[0]; typ=raw[pos+4:pos+8]; dat=raw[pos+8:pos+8+n]; pos += 12+n
        if typ==b"IHDR":
            width,height,depth,ctype,comp,filt,interlace=struct.unpack(">IIBBBBB",dat)
            if depth!=8 or interlace!=0 or ctype not in (2,6): raise ValueError(f"unsupported PNG format depth={depth} ctype={ctype} interlace={interlace}")
        elif typ==b"IDAT": idat += dat
        elif typ==b"IEND": break
    bpp=4 if ctype==6 else 3
    data=zlib.decompress(idat); stride=width*bpp; prev=bytearray(stride); out=[]; off=0
    for _ in range(height):
        ft=data[off]; off+=1; src=bytearray(data[off:off+stride]); off+=stride; row=bytearray(stride)
        for i,x in enumerate(src):
            a=row[i-bpp] if i>=bpp else 0; b=prev[i]; c=prev[i-bpp] if i>=bpp else 0
            if ft==0: v=x
            elif ft==1: v=(x+a)&255
            elif ft==2: v=(x+b)&255
            elif ft==3: v=(x+((a+b)//2))&255
            elif ft==4: v=(x+paeth(a,b,c))&255
            else: raise ValueError(f"bad filter {ft}")
            row[i]=v
        rgba=[]
        for i in range(0,stride,bpp):
            if bpp==4: rgba.extend(row[i:i+4])
            else: rgba.extend((*row[i:i+3],255))
        out.append(bytearray(rgba)); prev=row
    return width,height,out

def chunk(typ: bytes, data: bytes):
    return struct.pack(">I",len(data))+typ+data+struct.pack(">I",zlib.crc32(typ+data)&0xffffffff)

def encode_png(width,height,rows):
    payload=b"".join(b"\x00"+bytes(r) for r in rows)
    ihdr=struct.pack(">IIBBBBB",width,height,8,6,0,0,0)
    return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",ihdr)+chunk(b"IDAT",zlib.compress(payload,9))+chunk(b"IEND",b"")

def gradient(t: float):
    t=max(0.0,min(1.0,t))
    for (a,ca),(b,cb) in zip(PALETTE,PALETTE[1:]):
        if t<=b:
            u=(t-a)/(b-a) if b>a else 0
            return tuple(round(ca[i]+(cb[i]-ca[i])*u) for i in range(3))
    return PALETTE[-1][1]

def recolor(raw: bytes) -> bytes:
    w,h,rows=decode_png(raw)
    for row in rows:
        for i in range(0,len(row),4):
            r,g,b,a=row[i:i+4]
            if a==0: continue
            lum=(0.2126*r+0.7152*g+0.0722*b)/255.0
            rr,gg,bb=gradient(lum)
            row[i:i+4]=bytes((rr,gg,bb,a))
    return encode_png(w,h,rows)

def main():
    index=fetch_asset_index()
    for key,out in ASSETS.items():
        raw=fetch_object(index,key)
        out.parent.mkdir(parents=True,exist_ok=True)
        out.write_bytes(recolor(raw))
        print(f"generated {out.relative_to(ROOT)} from official Mojang asset {key}")

if __name__ == "__main__": main()
