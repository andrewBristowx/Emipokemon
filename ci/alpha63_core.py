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

print("alpha.63 interactive claw, transparent ticket, Pokémon portraits and distinct world models applied")
