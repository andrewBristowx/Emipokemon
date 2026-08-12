from pathlib import Path, PurePosixPath
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
source = root / "ci" / "alpha67_overlay.zip"
target = Path.cwd().resolve()

if "mod_version=0.4.0-alpha.66" not in (target / "gradle.properties").read_text(encoding="utf-8"):
    raise SystemExit("alpha67 must be applied after the exact alpha66 candidate")

with ZipFile(source) as archive:
    for member in archive.infolist():
        relative = PurePosixPath(member.filename.replace("\\", "/"))
        if relative.is_absolute() or ".." in relative.parts:
            raise SystemExit(f"unsafe alpha67 overlay member: {member.filename!r}")
        destination = target.joinpath(*relative.parts)
        if member.is_dir():
            destination.mkdir(parents=True, exist_ok=True)
            continue
        destination.parent.mkdir(parents=True, exist_ok=True)
        with archive.open(member) as source_file, destination.open("wb") as output:
            output.write(source_file.read())

# The rejected MP4 conversion is removed from both code and packaged resources.
reveal = target / "src/main/resources/assets/emipokemon/textures/gui/gacha/reveal_sheet.png"
reveal.unlink(missing_ok=True)

if "mod_version=0.4.0-alpha.67" not in (target / "gradle.properties").read_text(encoding="utf-8"):
    raise SystemExit("alpha67 version was not applied")

print("alpha67 visual rollback, portrait fix and real LuckPerms groups applied")
