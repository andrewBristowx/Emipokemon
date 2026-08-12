from math import cos, pi, sin
from pathlib import Path

from PIL import Image


root = Path(".")
ci = Path(__file__).resolve().parent
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")

for marker in (
    "drawSlotShowcase", "drawChipExchangeShowcase", "drawTicketShowcase",
    "drawDiceShowcase", "drawCardShowcase", "CasinoTheme",
):
    assert marker in screen, marker

for game in ("slot", "chip_exchange", "ticket_exchange", "dice", "blackjack", "poker"):
    assert f'"{game}"' in screen, game

wheel = Image.open(root / "src/main/resources/assets/emipokemon/textures/gui/casino/roulette_alpha51_wheel.png").convert("RGBA")
original = Image.open(ci / "assets/roulette_alpha51_wheel.png").convert("RGBA")
center = wheel.width / 2.0
for radius in (344.0, 442.0):
    for degrees in range(0, 360, 15):
        angle = degrees * pi / 180.0
        point = (round(center + cos(angle) * radius), round(center + sin(angle) * radius))
        assert wheel.getpixel(point) == original.getpixel(point), (radius, degrees)

order = [0,32,15,19,4,21,2,25,17,34,6,27,13,36,11,30,8,23,10,5,24,16,33,1,20,14,31,9,22,18,29,7,28,12,35,3,26]
red = {1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36}
rgb_wheel = wheel.convert("RGB")
for i, number in enumerate(order):
    angle = -pi / 2.0 + 2.0 * pi * i / len(order)
    rgb = rgb_wheel.getpixel((round(center + cos(angle) * 390.0), round(center + sin(angle) * 390.0)))
    actual = "green" if rgb[1] > rgb[0] else "red" if rgb[0] > 100 else "black"
    expected = "green" if number == 0 else "red" if number in red else "black"
    assert actual == expected, (number, expected, actual, rgb)

assert "mod_version=0.4.0-alpha.57" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.57 personalized visuals, restored arcs and canonical colors checks passed")
