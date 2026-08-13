from pathlib import Path, PurePosixPath
from zipfile import ZipFile
from io import BytesIO
import base64
import hashlib

root = Path(__file__).resolve().parents[1]
ci = root / "ci"
target = Path.cwd().resolve()

parts = [ci / f"alpha70_overlay_{index:02d}.b64part" for index in range(6)]
missing = [part.name for part in parts if not part.is_file()]
if missing:
    raise SystemExit(f"alpha70 overlay chunks missing: {missing}")
encoded = "".join(part.read_text(encoding="ascii") for part in parts)
archive_bytes = base64.b64decode(encoded, validate=True)
if hashlib.sha256(archive_bytes).hexdigest() != "b53fe78369b1bff8616300ec4b399e6567138f40274d17e388c6224e3529dc06":
    raise SystemExit("alpha70 overlay checksum mismatch")

properties = (target / "gradle.properties").read_text(encoding="utf-8")
if "mod_version=0.4.0-alpha.69" not in properties:
    raise SystemExit("alpha70 must be applied after the exact alpha69 candidate")

with ZipFile(BytesIO(archive_bytes)) as archive:
    for member in archive.infolist():
        relative = PurePosixPath(member.filename.replace("\\", "/"))
        if relative.is_absolute() or ".." in relative.parts:
            raise SystemExit(f"unsafe alpha70 overlay member: {member.filename!r}")
        destination = target.joinpath(*relative.parts)
        if member.is_dir():
            destination.mkdir(parents=True, exist_ok=True)
            continue
        destination.parent.mkdir(parents=True, exist_ok=True)
        with archive.open(member) as source_file, destination.open("wb") as output:
            output.write(source_file.read())

if "mod_version=0.4.0-alpha.70" not in (target / "gradle.properties").read_text(encoding="utf-8"):
    raise SystemExit("alpha70 version was not applied")
print("alpha70 gacha portraits, rates, seasonal display and claw shell fixes applied")
