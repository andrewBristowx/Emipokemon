from pathlib import Path
import struct, zlib

root = Path('.')

def png(width, height, pixels):
    raw = b''.join(b'\x00' + bytes(pixels[y * width * 4:(y + 1) * width * 4]) for y in range(height))
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    return (b'\x89PNG\r\n\x1a\n'
            + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9))
            + chunk(b'IEND', b''))

def atlas(path, palette, kind):
    w = h = 128
    px = bytearray(w * h * 4)

    def rect(x0, y0, x1, y1, color):
        r, g, b = color
        for y in range(max(0, y0), min(h, y1)):
            for x in range(max(0, x0), min(w, x1)):
                i = (y * w + x) * 4
                px[i:i+4] = bytes((r, g, b, 255))

    def stripe(x0, y0, x1, y1, color, step=6):
        for y in range(y0, y1, step):
            rect(x0, y, x1, min(y + 1, y1), color)

    body, accent, surface, light, dark, detail = palette

    # Same six UV zones as alpha.36, but with brighter base values and restrained detailing.
    rect(0, 0, 46, 46, body)
    rect(48, 0, 86, 46, accent)
    rect(0, 48, 46, 88, surface)
    rect(48, 48, 86, 88, light)
    rect(88, 0, 127, 46, dark)
    rect(88, 48, 127, 88, detail)
    rect(0, 90, 127, 127, body)

    # Fine seams make large cabinet faces readable without turning into noisy checkerboards.
    stripe(1, 1, 45, 45, tuple(max(0, c - 12) for c in body), 8)
    stripe(49, 1, 85, 45, tuple(max(0, c - 15) for c in accent), 9)
    stripe(1, 49, 45, 87, tuple(max(0, c - 10) for c in surface), 10)

    # Machine-specific icon details placed inside the detail/light regions used by small bones.
    if kind == 'slot':
        rect(54, 55, 60, 72, (210, 55, 95)); rect(63, 55, 69, 72, (246, 190, 45)); rect(72, 55, 78, 72, (55, 176, 170))
    elif kind == 'chip_exchange':
        for r in range(4): rect(95 + r * 6, 56 + r * 2, 100 + r * 6, 61 + r * 2, (248, 210, 82))
    elif kind == 'ticket_exchange':
        rect(93, 56, 121, 72, (250, 238, 205)); rect(96, 59, 118, 62, (222, 92, 130)); rect(96, 66, 112, 69, (75, 150, 190))
    elif kind == 'roulette':
        rect(52, 54, 82, 82, (25, 112, 69)); rect(60, 54, 64, 82, (204, 46, 54)); rect(70, 54, 74, 82, (33, 33, 38))
    elif kind in ('poker', 'blackjack'):
        rect(52, 54, 82, 84, (31, 119, 72)); rect(57, 58, 67, 75, (247, 239, 216)); rect(69, 58, 79, 75, (247, 239, 216))
    elif kind == 'dice':
        rect(54, 54, 68, 68, (248, 243, 226)); rect(71, 68, 85, 82, (248, 243, 226))
        for x, y in [(58, 58), (64, 64), (75, 72), (81, 78)]: rect(x, y, x + 2, y + 2, (35, 28, 45))

    p = root / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes(png(w, h, px))

# Professional, distinct machine identities. None uses near-black as the main body color and
# none uses neon magenta as the dominant accent.
palettes = {
    'slot': (
        (142, 58, 112), (235, 174, 55), (91, 54, 128),
        (252, 235, 170), (79, 43, 77), (56, 171, 168)),
    'chip_exchange': (
        (45, 122, 116), (232, 181, 70), (218, 233, 208),
        (250, 238, 182), (43, 76, 78), (186, 83, 132)),
    'ticket_exchange': (
        (217, 176, 154), (222, 91, 127), (244, 213, 199),
        (252, 232, 169), (112, 62, 76), (76, 157, 190)),
    'roulette': (
        (133, 84, 54), (225, 177, 68), (32, 116, 72),
        (245, 229, 190), (73, 48, 38), (190, 57, 58)),
    'poker': (
        (119, 75, 50), (214, 169, 68), (30, 112, 67),
        (245, 232, 199), (70, 48, 39), (143, 57, 73)),
    'blackjack': (
        (57, 78, 118), (220, 174, 69), (31, 114, 70),
        (248, 237, 207), (42, 52, 78), (176, 63, 75)),
    'dice': (
        (132, 61, 82), (222, 176, 68), (37, 112, 75),
        (247, 239, 218), (75, 48, 61), (107, 83, 147)),
}

for kind, palette in palettes.items():
    atlas(f'src/main/resources/assets/emipokemon/textures/block/casino_{kind}.png', palette, kind)
