from pathlib import Path


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")

required = (
    "size * 0.286F",
    "Math.max(0.72F",
    "0.398D - 0.108D * eased",
    "drawScaledIntegratedWrapped",
    "context.enableScissor",
    "context.disableScissor",
    "refX(1172)",
    "refY(865)",
    "refY(918)",
    "rouletteResultNumber()",
)
for marker in required:
    assert marker in screen, marker

for forbidden in (
    "size * 0.334F",
    "0.456D - 0.118D * eased",
    "sideX + sidePx(33), contentTop + sidePy(507)",
):
    assert forbidden not in screen, forbidden

assert "mod_version=0.4.0-alpha.53" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.53 wheel groove and side-panel bounds checks passed")
