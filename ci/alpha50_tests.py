from pathlib import Path
from PIL import Image


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")

required = [
    "ROULETTE_REFERENCE_W = 1535",
    "ROULETTE_REFERENCE_H = 1024",
    "referenceAspect",
    "width - 24",
    "height - 24",
    "drawAsset(context, ROULETTE_REFERENCE",
    "ROULETTE_SIDE_SOURCE_X / (float)ROULETTE_REFERENCE_W",
    "double angle = TAU * i / ROULETTE_WHEEL.length",
    "sidePx(227)",
    "sidePx(61)",
]
for marker in required:
    assert marker in screen, marker

for forbidden in (
    "contentTop -= rouletteOverlapH",
    "double angle = rotation + TAU",
    "drawCircle(context, bx, by",
    "int tileW = Math.max(10, leftPx(15))",
):
    assert forbidden not in screen, forbidden

reference = root / "src/main/resources/assets/emipokemon/textures/gui/casino/roulette_reference.png"
with Image.open(reference) as image:
    assert image.size == (1535, 1024), image.size

assert "mod_version=0.4.0-alpha.50" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.50 viewport and roulette visual checks passed")
