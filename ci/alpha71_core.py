from pathlib import Path, PurePosixPath
from zipfile import ZipFile
from io import BytesIO
import base64
import hashlib

root = Path(__file__).resolve().parents[1]
ci = root / "ci"
target = Path.cwd().resolve()

part_names = ["00", "01", "02", "03", "04a", "04b0", "04b1", "05a", "05b"]
parts = [ci / f"alpha71_overlay_{name}.b64part" for name in part_names]
missing = [part.name for part in parts if not part.is_file()]
if missing:
    raise SystemExit(f"alpha71 overlay chunks missing: {missing}")
encoded = "".join(part.read_text(encoding="ascii") for part in parts)
archive_bytes = base64.b64decode(encoded, validate=True)
if hashlib.sha256(archive_bytes).hexdigest() != "6347efbaceeee1a2f6766ee77f29c077b5c751762f2158f4cf600167c77f84e1":
    raise SystemExit("alpha71 overlay checksum mismatch")

properties = (target / "gradle.properties").read_text(encoding="utf-8")
if "mod_version=0.4.0-alpha.70" not in properties:
    raise SystemExit("alpha71 must be applied after the exact alpha70 candidate")

with ZipFile(BytesIO(archive_bytes)) as archive:
    for member in archive.infolist():
        relative = PurePosixPath(member.filename.replace("\\", "/"))
        if relative.is_absolute() or ".." in relative.parts:
            raise SystemExit(f"unsafe alpha71 overlay member: {member.filename!r}")
        destination = target.joinpath(*relative.parts)
        if member.is_dir():
            destination.mkdir(parents=True, exist_ok=True)
            continue
        destination.parent.mkdir(parents=True, exist_ok=True)
        with archive.open(member) as source_file, destination.open("wb") as output:
            output.write(source_file.read())

if "mod_version=0.4.0-alpha.71" not in (target / "gradle.properties").read_text(encoding="utf-8"):
    raise SystemExit("alpha71 version was not applied")
print("alpha71 portrait API, seasonal sync/height and claw seam fixes applied")
