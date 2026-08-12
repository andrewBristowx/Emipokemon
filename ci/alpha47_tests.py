from pathlib import Path
import struct
root=Path('.')
base=root/'src/main/resources/assets/emipokemon/textures/gui/casino'
expected={
 'roulette_header.png':(1535,146),
 'roulette_left_panel.png':(1052,828),
 'roulette_side_panel.png':(440,828),
 'roulette_wheel_outer.png':(410,410),
 'roulette_medallion.png':(200,200),
}
for name,(w,h) in expected.items():
    p=base/name
    data=p.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', name
    iw,ih=struct.unpack('>II',data[16:24])
    assert (iw,ih)==(w,h),(name,(iw,ih),(w,h))
    assert len(data)>12000,(name,len(data))

s=(root/'src/client/java/com/emipokemon/client/casino/CasinoScreen.java').read_text()
for token in [
 'ROULETTE_HEADER_TEX_W = 1535',
 'ROULETTE_LEFT_TEX_W = 1052',
 'ROULETTE_SIDE_TEX_W = 440',
 'ROULETTE_WHEEL_TEX_SIZE = 410',
 'ROULETTE_MEDALLION_TEX_SIZE = 200',
 'rouletteHeaderH = Math.max(52',
 'panelH - rouletteHeaderH',
 'send(cell.action)',
 'state.recentResults()',
 'public void renderBackground(DrawContext context',
]: assert token in s, token
assert 'drawAsset(context, ROULETTE_HEADER, panelX, panelY, panelW, 76' not in s
assert 'ROULETTE_LEFT_TEX_W = 182' not in s
print('alpha47 HD roulette checks passed')
