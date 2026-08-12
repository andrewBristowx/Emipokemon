from pathlib import Path
from PIL import Image
from math import cos, pi, sin


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")
assert "double radius = wheelSize * 0.370D" in screen
assert "wheelSize * 0.398D" not in screen
assert "drawReadableText" in screen
assert "0xFFE2D8C7" in screen
assert '"oversample": 4.0' in (root / "src/main/resources/assets/emipokemon/font/casino.json").read_text(encoding="utf-8")

order = [0,32,15,19,4,21,2,25,17,34,6,27,13,36,11,30,8,23,10,5,24,16,33,1,20,14,31,9,22,18,29,7,28,12,35,3,26]
red_numbers = {1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36}
image = Image.open(root / "src/main/resources/assets/emipokemon/textures/gui/casino/roulette_alpha51_wheel.png").convert("RGB")
center = image.width / 2.0
for i, number in enumerate(order):
    angle = -pi / 2.0 + 2.0 * pi * i / len(order)
    rgb = image.getpixel((round(center + cos(angle) * 390.0), round(center + sin(angle) * 390.0)))
    actual = "green" if rgb[1] > rgb[0] else "red" if rgb[0] > 100 else "black"
    expected = "green" if number == 0 else "red" if number in red_numbers else "black"
    assert actual == expected, (number, expected, actual, rgb)

assert order[-1] == 26 and 26 not in red_numbers
assert "mod_version=0.4.0-alpha.56" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.56 canonical color, brown-track and legibility checks passed")
