from pathlib import Path

root = Path('.')
expected = root / 'src/client/java/com/emipokemon/EmipokemonClient.java'
if expected.exists():
    raise SystemExit(0)

matches = []
for path in (root / 'src').rglob('*.java'):
    try:
        text = path.read_text()
    except Exception:
        continue
    if 'ClientModInitializer' in text and 'onInitializeClient()' in text:
        matches.append(path)

if len(matches) != 1:
    raise SystemExit(f'expected exactly one ClientModInitializer, found: {matches}')

expected.parent.mkdir(parents=True, exist_ok=True)
expected.symlink_to(matches[0].resolve())
print(f'alpha32 client initializer: {matches[0]}')
