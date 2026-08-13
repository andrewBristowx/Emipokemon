from pathlib import Path

renderer = Path('src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java').read_text(encoding='utf-8')
props = Path('gradle.properties').read_text(encoding='utf-8')
assert '3.30' in renderer
assert '0.040' in renderer
assert 'mod_version=0.4.0-alpha.72' in props
print('alpha72 text regression preflight passed')
