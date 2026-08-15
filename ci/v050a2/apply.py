from pathlib import Path
import base64
import hashlib
import io
import zipfile

root = Path(__file__).resolve().parent
payload = "".join((root / f"payload.{i:02d}").read_text().strip() for i in range(8))
data = base64.b64decode(payload)
expected = "dc00c4eb030f4fd9951391dedce09bd87fe7222edf9a54b604b9d36e998b6b94"
actual = hashlib.sha256(data).hexdigest()
if actual != expected:
    raise SystemExit(f"alpha.2 payload hash mismatch: {actual} != {expected}")

with zipfile.ZipFile(io.BytesIO(data)) as archive:
    names = archive.namelist()
    for name in names:
        target = Path(name)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(archive.read(name))

print(f"Applied Emipokemon 0.5.0-alpha.2 patch ({len(names)} files).")
