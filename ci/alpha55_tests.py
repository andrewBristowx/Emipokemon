from pathlib import Path


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")

required = (
    "double radius = wheelSize * 0.398D",
    "double resultAngle = TAU * resultIndex / ROULETTE_WHEEL.length",
    "ROULETTE_WHEEL[i] == result",
    "panelX + refX(1262)",
    "refX(145)",
    "panelX + refX(1182)",
    "panelY + refY(520)",
    "panelY + refY(544)",
)
for marker in required:
    assert marker in screen, marker

for forbidden in (
    "rouletteWheelRotation()",
    "rotation((float)rotation)",
    "0.398D - 0.108D * eased",
    "panelY + refY(541)",
    "panelY + refY(563)",
):
    assert forbidden not in screen, forbidden

assert "mod_version=0.4.0-alpha.55" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.55 ball-only and adaptive text checks passed")
