from pathlib import Path
import shutil

root = Path(".")
overlay = Path(__file__).resolve().parent / "alpha63_overlay"

properties = (root / "gradle.properties").read_text(encoding="utf-8")
if "mod_version=0.4.0-alpha.62" not in properties:
    raise AssertionError("alpha.63 must be applied to the exact alpha.62 source candidate")

for source in overlay.rglob("*"):
    if not source.is_file():
        continue
    target = root / source.relative_to(overlay)
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)

(root / "gradle.properties").write_text(
    properties.replace("mod_version=0.4.0-alpha.62", "mod_version=0.4.0-alpha.63"),
    encoding="utf-8",
)

for path in (root / "src").rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    if "0.4.0-alpha.62" in text:
        path.write_text(text.replace("0.4.0-alpha.62", "0.4.0-alpha.63"), encoding="utf-8")

font_license = root / "src/main/resources/assets/emipokemon/font/OFL-PixelifySans.txt"
if not font_license.is_file():
    raise AssertionError("alpha.62 source candidate is missing the bundled Pixelify Sans license")
license_copy = root / "LICENSES/OFL-PixelifySans.txt"
license_copy.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(font_license, license_copy)

print("alpha.63 interactive claw, transparent ticket, Pokémon portraits and distinct world models applied")
