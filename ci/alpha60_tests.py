from pathlib import Path
from PIL import Image


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")
for marker in (
    "CASINO_SLOT_SYMBOLS", "SLOT_SYMBOL_ATLAS_W", "drawFinishedControlLabel",
    "hasDiceResult() && elapsed < 2200L", "float bounce", "float travel", "float angle",
    '{"under7", "exact7", "over7"}',
):
    assert marker in screen, marker
for symbol in ("CEREZA", "BAYA", "CAMPANA", "ESTRELLA", "EMI", "JACKPOT"):
    assert symbol in screen, symbol
asset = root / "src/main/resources/assets/emipokemon/textures/gui/casino/finished/slot_symbols.png"
with Image.open(asset) as image:
    assert image.size == (1280, 286), image.size
    assert image.mode == "RGBA", image.mode
assert "mod_version=0.4.0-alpha.60" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.60 visual atlas, text layout and dice animation checks passed")
