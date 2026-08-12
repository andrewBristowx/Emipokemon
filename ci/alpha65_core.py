from pathlib import Path, PurePosixPath
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
source = root / "ci" / "alpha65_overlay.zip"
target = Path.cwd().resolve()

if not source.is_file():
    raise SystemExit("alpha65 overlay is missing")

with ZipFile(source) as archive:
    for member in archive.infolist():
        relative = PurePosixPath(member.filename.replace("\\", "/"))
        if relative.is_absolute() or ".." in relative.parts:
            raise SystemExit(f"unsafe alpha65 overlay member: {member.filename!r}")
        destination = target.joinpath(*relative.parts)
        if member.is_dir():
            destination.mkdir(parents=True, exist_ok=True)
            continue
        destination.parent.mkdir(parents=True, exist_ok=True)
        with archive.open(member) as source_file, destination.open("wb") as output:
            output.write(source_file.read())

properties = (target / "gradle.properties").read_text(encoding="utf-8")
if "mod_version=0.4.0-alpha.65" not in properties:
    raise SystemExit("alpha65 version was not applied")

print("alpha65 overlay applied")
