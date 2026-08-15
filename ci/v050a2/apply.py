from pathlib import Path
import base64
import hashlib
import io
import zipfile

root = Path(__file__).resolve().parent
parts = [
    "payload.00",
    "payload.01a", "payload.01b1", "payload.01b2", "payload.01b3", "payload.01c",
    "payload.02", "payload.03", "payload.04",
    "payload.05a", "payload.05b", "payload.05c",
    "payload.06", "payload.07",
]
payload = "".join((root / name).read_text().strip() for name in parts)
data = base64.b64decode(payload)
expected = "dc00c4eb030f4fd9951391dedce09bd87fe7222edf9a54b604b9d36e998b6b94"
actual = hashlib.sha256(data).hexdigest()
if actual != expected:
    raise SystemExit(f"alpha.2 payload hash mismatch: {actual} != {expected}")

with zipfile.ZipFile(io.BytesIO(data)) as archive:
    names = archive.namelist()
    for name in names:
        content = archive.read(name)  # validates each ZIP entry CRC
        target = Path(name)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)

print(f"Applied exact Emipokemon 0.5.0-alpha.2 patch ({len(names)} files).")
