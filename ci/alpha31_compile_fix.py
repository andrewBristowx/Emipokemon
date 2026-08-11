from pathlib import Path
import re

path = Path('src/main/java/com/emipokemon/hologram/HologramViewerTextService.java')
source = path.read_text()
source, count = re.subn(
    r'^\s*String fingerprint =.*;$',
    '                String fingerprint = entry.text() + "|" + viewerText.getString() + "|" + entry.color();',
    source,
    count=1,
    flags=re.MULTILINE,
)
if count != 1:
    raise SystemExit('alpha31 fingerprint line not found')
path.write_text(source)
