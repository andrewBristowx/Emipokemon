from pathlib import Path, PurePosixPath
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
source = root / "ci" / "alpha66_overlay.zip"
target = Path.cwd().resolve()

if not source.is_file():
    raise SystemExit("alpha66 overlay is missing")

properties = (target / "gradle.properties").read_text(encoding="utf-8")
if "mod_version=0.4.0-alpha.65" not in properties:
    raise SystemExit("alpha66 must be applied after the exact alpha65 candidate")

with ZipFile(source) as archive:
    for member in archive.infolist():
        relative = PurePosixPath(member.filename.replace("\\", "/"))
        if relative.is_absolute() or ".." in relative.parts:
            raise SystemExit(f"unsafe alpha66 overlay member: {member.filename!r}")
        destination = target.joinpath(*relative.parts)
        if member.is_dir():
            destination.mkdir(parents=True, exist_ok=True)
            continue
        destination.parent.mkdir(parents=True, exist_ok=True)
        with archive.open(member) as source_file, destination.open("wb") as output:
            output.write(source_file.read())

asset_source = root / "ci" / "alpha66_assets"
asset_target = target / "src/main/resources/assets/emipokemon/textures/gui/gacha"
asset_target.mkdir(parents=True, exist_ok=True)
for name in ("standard_gacha.png", "emi_gacha.png", "reveal_sheet.png"):
    source_file = asset_source / name
    if not source_file.is_file():
        raise SystemExit(f"alpha66 asset is missing: {name}")
    (asset_target / name).write_bytes(source_file.read_bytes())

if "mod_version=0.4.0-alpha.66" not in (target / "gradle.properties").read_text(encoding="utf-8"):
    raise SystemExit("alpha66 version was not applied")

print("alpha66 gacha UI, kits, admin controls and machine textures applied")
