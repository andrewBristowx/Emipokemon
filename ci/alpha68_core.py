from pathlib import Path, PurePosixPath
from zipfile import ZipFile
from io import BytesIO
import base64

root = Path(__file__).resolve().parents[1]
source = root / "ci" / "alpha68_overlay.zip.b64"
target = Path.cwd().resolve()

if not source.is_file():
    raise SystemExit("alpha68 overlay is missing")

properties = (target / "gradle.properties").read_text(encoding="utf-8")
if "mod_version=0.4.0-alpha.67" not in properties:
    raise SystemExit("alpha68 must be applied after the exact alpha67 candidate")

with ZipFile(BytesIO(base64.b64decode(source.read_text(encoding="ascii")))) as archive:
    for member in archive.infolist():
        relative = PurePosixPath(member.filename.replace("\\", "/"))
        if relative.is_absolute() or ".." in relative.parts:
            raise SystemExit(f"unsafe alpha68 overlay member: {member.filename!r}")
        destination = target.joinpath(*relative.parts)
        if member.is_dir():
            destination.mkdir(parents=True, exist_ok=True)
            continue
        destination.parent.mkdir(parents=True, exist_ok=True)
        with archive.open(member) as source_file, destination.open("wb") as output:
            output.write(source_file.read())

if "mod_version=0.4.0-alpha.68" not in (target / "gradle.properties").read_text(encoding="utf-8"):
    raise SystemExit("alpha68 version was not applied")

print("alpha68 Emi tools, gacha layout and seasonal machine display applied")
