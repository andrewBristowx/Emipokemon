from pathlib import Path
import shutil

root = Path(".")
overlay = Path(__file__).resolve().parent / "alpha64_overlay"

properties = (root / "gradle.properties").read_text(encoding="utf-8")
if "mod_version=0.4.0-alpha.63" not in properties:
    raise AssertionError("alpha.64 must be applied after the exact alpha.63 candidate")

for source in overlay.rglob("*"):
    if not source.is_file():
        continue
    target = root / source.relative_to(overlay)
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)

if "mod_version=0.4.0-alpha.64" not in (root / "gradle.properties").read_text(encoding="utf-8"):
    raise AssertionError("alpha.64 overlay did not set the candidate version")

font_license = root / "src/main/resources/assets/emipokemon/font/OFL-PixelifySans.txt"
if not font_license.is_file():
    raise AssertionError("candidate is missing the bundled Pixelify Sans license")
license_copy = root / "LICENSES/OFL-PixelifySans.txt"
license_copy.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(font_license, license_copy)

print("alpha.64 daily rewards, infinite pass, virtual pulls and casino corrections applied")
