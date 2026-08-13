from pathlib import Path


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")
for marker in (
    "drawSlotSymbol", "drawAnimatedDie", "drawCardFace", "drawSuit",
    "stopAt = 850L + i * 300L", "elapsed - baseDelay - i * 125L",
    '{"under7", "exact7", "over7"}',
):
    assert marker in screen, marker
assert "mod_version=0.4.0-alpha.59" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.59 animation, symbol, card and dice checks passed")
