#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()

def replace_once(path: Path, old: str, new: str):
    text = path.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'alpha74 compat snippet missing in {path}: {old!r}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

replace_once(
    root / 'src/test/java/com/emipokemon/alpha73/Alpha73WorldLabelsAndChatFocusRegressionTest.java',
    'assertTrue(service.contains("nbt.putString(\\"billboard\\", \\"center\\")"));',
    'assertTrue(service.contains("nbt.putString(\\"billboard\\", \\"fixed\\")"));'
)
replace_once(
    root / 'src/test/java/com/emipokemon/alpha71/Alpha71VisualSyncRegressionTest.java',
    'assertTrue(renderer.contains("2.78D + bob"));',
    'assertTrue(renderer.contains("4.18D + bob"));'
)
replace_once(
    root / 'src/test/java/com/emipokemon/alpha69/Alpha69GachaFixesRegressionTest.java',
    'assertTrue(renderer.contains("1.45F / width"));',
    'assertTrue(renderer.contains("1.25F / width"));'
)
replace_once(
    root / 'src/test/java/com/emipokemon/alpha69/Alpha69GachaFixesRegressionTest.java',
    'assertTrue(renderer.contains("1.35F / height"));',
    'assertTrue(renderer.contains("1.05F / height"));'
)
print('alpha.74 inherited regression expectations updated')
