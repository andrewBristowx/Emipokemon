from pathlib import Path
from zipfile import ZipFile
import struct

root = Path(__file__).resolve().parents[1]
overlay = root / "ci" / "alpha66_overlay.zip"

with ZipFile(overlay) as archive:
    names = set(archive.namelist())
    required = {
        "src/main/java/com/emipokemon/gacha/GachaNetworking.java",
        "src/client/java/com/emipokemon/client/gacha/GachaScreen.java",
        "src/client/java/com/emipokemon/client/gacha/GachaClient.java",
        "src/main/java/com/emipokemon/rewards/KitCommands.java",
    }
    missing = sorted(required - names)
    if missing:
        raise SystemExit(f"alpha66 overlay missing: {missing}")

    expected_sizes = {
        "src/main/resources/assets/emipokemon/textures/gui/gacha/standard_gacha.png": (1536, 1024),
        "src/main/resources/assets/emipokemon/textures/gui/gacha/emi_gacha.png": (1536, 1024),
        "src/main/resources/assets/emipokemon/textures/gui/gacha/reveal_sheet.png": (3072, 1296),
    }
    for name, expected in expected_sizes.items():
        data = (root / "ci" / "alpha66_assets" / Path(name).name).read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n":
            raise SystemExit(f"{name} is not PNG")
        actual = struct.unpack(">II", data[16:24])
        if actual != expected:
            raise SystemExit(f"{name} has {actual}, expected {expected}")

print("alpha66 overlay validated")
