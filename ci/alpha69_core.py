from pathlib import Path
import shutil

root = Path(__file__).resolve().parents[1]
source = root / "ci" / "alpha69_files"
target = Path.cwd().resolve()

if not source.is_dir():
    raise SystemExit("alpha69 source overlay is missing")

properties = (target / "gradle.properties").read_text(encoding="utf-8")
if "mod_version=0.4.0-alpha.68" not in properties:
    raise SystemExit("alpha69 must be applied after the exact alpha68 candidate")

for item in source.rglob("*"):
    if not item.is_file():
        continue
    relative = item.relative_to(source)
    destination = target / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(item, destination)

if "mod_version=0.4.0-alpha.69" not in (target / "gradle.properties").read_text(encoding="utf-8"):
    raise SystemExit("alpha69 version was not applied")

print("alpha69 gacha renderer, staggered reveal and featured rotation applied")
