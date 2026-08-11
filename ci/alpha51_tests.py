from pathlib import Path
from PIL import Image


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")

required = (
    "ROULETTE_ALPHA51_BACKGROUND",
    "ROULETTE_ALPHA51_WHEEL",
    "CASINO_FONT",
    "RotationAxis.POSITIVE_Z.rotation",
    "ROULETTE_WHEEL.length",
    "rouletteResultNumber()",
    '"result".equals(state.phase())',
    "TAU * 7.0D + target",
    "drawRouletteBall(context, size)",
    "new QuickChipZone(panelX + refX(425)",
    "new RouletteCell(zeroX",
    "adjustAmount(-1)",
    "adjustAmount(1)",
    "count + \"/\" + ROULETTE_DISPLAY_CAPACITY",
)
for marker in required:
    assert marker in screen, marker

for forbidden in (
    "drawAsset(context, ROULETTE_REFERENCE",
    "drawAsset(context, ROULETTE_LEFT_PANEL",
    "drawAsset(context, ROULETTE_SIDE_PANEL",
    "drawAsset(context, ROULETTE_HEADER",
    "double angle = rotation + TAU",
):
    assert forbidden not in screen, forbidden

asset_root = root / "src/main/resources/assets/emipokemon"
with Image.open(asset_root / "textures/gui/casino/roulette_alpha51_background.png") as image:
    assert image.size == (1536, 1024), image.size
    assert image.mode == "RGB", image.mode
with Image.open(asset_root / "textures/gui/casino/roulette_alpha51_wheel.png") as image:
    assert image.size == (1254, 1254), image.size
    assert image.mode == "RGBA", image.mode
    assert image.getchannel("A").getextrema() == (0, 255)

font = asset_root / "font/pixelify_sans.ttf"
assert font.stat().st_size > 70_000
assert (asset_root / "font/casino.json").is_file()
assert (asset_root / "font/OFL-PixelifySans.txt").is_file()
assert (root / "LICENSES/OFL-PixelifySans.txt").is_file()
assert "mod_version=0.4.0-alpha.51" in (root / "gradle.properties").read_text(encoding="utf-8")

print("alpha.51 roulette assets, hitboxes, font and authoritative animation checks passed")
