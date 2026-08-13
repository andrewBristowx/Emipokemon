from pathlib import Path

root = Path('.')
renderer = root / 'src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java'
text = renderer.read_text(encoding='utf-8')
original = text

# Text-only visual validation pass: keep Pokemon/reward logic intact.
# Alpha.71 positioned the label at Y 4.18; alpha.72 brings it close to the cabinet roof.
repls = [
    ('4.18D', '3.30D'),
    ('4.18d', '3.30d'),
    ('4.18F', '3.30F'),
    ('4.18f', '3.30f'),
    ('4.18', '3.30'),
    ('0.025F', '0.040F'),
    ('0.025f', '0.040f'),
    ('TextRenderer.TextLayerType.NORMAL', 'TextRenderer.TextLayerType.SEE_THROUGH'),
]
for old, new in repls:
    if old in text:
        text = text.replace(old, new)

if text == original:
    raise SystemExit('alpha72: no expected alpha71 text-render tokens found')
if '3.30' not in text:
    raise SystemExit('alpha72: text Y was not moved to 3.30')
if '0.040' not in text:
    raise SystemExit('alpha72: text scale was not increased to 0.040')
renderer.write_text(text, encoding='utf-8')

props = root / 'gradle.properties'
p = props.read_text(encoding='utf-8')
if 'mod_version=0.4.0-alpha.71' not in p:
    raise SystemExit('alpha72: expected alpha.71 version not found')
props.write_text(p.replace('mod_version=0.4.0-alpha.71', 'mod_version=0.4.0-alpha.72'), encoding='utf-8')

(root / 'CHANGELOG-0.4.0-alpha.72.md').write_text('''# Emipokemon 0.4.0-alpha.72

## Objetivo
Validar primero la visibilidad del texto flotante de la maquina Gacha de Emi.

## Cambios
- baja el texto destacado de Y 4.18 a Y 3.30
- aumenta su escala de 0.025 a 0.040
- usa SEE_THROUGH cuando el renderer anterior usaba NORMAL
- no modifica rates, pity, tickets, rotacion, Pokemon destacado ni el gacha normal

## Siguiente paso tras validacion visual
- anuncio bonito al cambiar el legendario de Emi
- eventos aleatorios exclusivos de Emi: shiny, habilidad oculta e IV perfectos
''', encoding='utf-8')

print('alpha72 text visibility patch applied')
