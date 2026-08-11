from pathlib import Path


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")

required = (
    "drawAsset(context, ROULETTE_ALPHA51_WHEEL",
    "panelX + refX(1245)",
    "panelX + refX(1194)",
    "panelX + refX(1424)",
    "panelY + refY(600)",
    "panelX + refX(1418)",
    "refX(218)",
    "rouletteResultNumber()",
)
for marker in required:
    assert marker in screen, marker

for forbidden in (
    "drawAsset(context, ROULETTE_MEDALLION",
    "panelX + refX(1143)",
    "panelX + refX(1172)",
    "panelY + refY(657)",
):
    assert forbidden not in screen, forbidden

assert "mod_version=0.4.0-alpha.54" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.54 composition and text-lane checks passed")
