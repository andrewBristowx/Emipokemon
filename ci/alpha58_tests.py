from pathlib import Path

from PIL import Image


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")
for marker in ("renderFinishedGame", "finishedActionControls", "drawFinishedGameState", "send(control.action())"):
    assert marker in screen, marker

assets = root / "src/main/resources/assets/emipokemon/textures/gui/casino/finished"
for game in ("poker", "blackjack", "dice", "slot", "chip_exchange", "ticket_exchange"):
    path = assets / f"casino_{game}.png"
    assert path.is_file(), path
    image = Image.open(path).convert("RGBA")
    assert image.size == (1536, 1024), (game, image.size)
    assert image.getpixel((0, 0))[3] == 255, game

assert "mod_version=0.4.0-alpha.58" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.58 finished assets, controls and version checks passed")
