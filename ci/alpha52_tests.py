from pathlib import Path


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")

required = (
    "size * 0.334F",
    "Math.max(0.62F",
    "refX(158)",
    "0.456D - 0.118D * eased",
    "drawFittedCenteredUiText(context, phaseLabel()",
    "badgeW - refX(12)",
    'drawFittedCenteredUiText(context, "Inicia en:"',
    "rouletteResultNumber()",
    "TAU * 7.0D + target",
)
for marker in required:
    assert marker in screen, marker

for forbidden in (
    "size * 0.386F",
    "refX(116)",
    "0.456D - 0.055D * eased",
    "drawCenteredUiText(context, phaseLabel()",
):
    assert forbidden not in screen, forbidden

font = (root / "src/main/resources/assets/emipokemon/font/casino.json").read_text(encoding="utf-8")
assert '"size": 9.0' in font
assert "mod_version=0.4.0-alpha.52" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.52 wheel, ball and typography alignment checks passed")
