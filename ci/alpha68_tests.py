from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
import base64
import hashlib

root = Path(__file__).resolve().parents[1]
candidate = Path.cwd()
overlay = root / "ci" / "alpha68_overlay.zip.b64"

required_overlay_paths = (
    "gradle.properties",
    "src/client/java/com/emipokemon/client/gacha/GachaScreen.java",
    "src/client/java/com/emipokemon/client/render/GachaMachineRenderer.java",
    "src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java",
    "src/main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java",
    "src/main/java/com/emipokemon/registry/ModRegistries.java",
    "src/main/java/com/emipokemon/tools/EmiToolActions.java",
    "src/main/java/com/emipokemon/tools/EmiToolMaterial.java",
    "src/test/java/com/emipokemon/alpha68/Alpha68EmiToolsAndSeasonDisplayRegressionTest.java",
)

with ZipFile(BytesIO(base64.b64decode(overlay.read_text(encoding="ascii")))) as archive:
    names = set(archive.namelist())
    for required in required_overlay_paths:
        if required not in names:
            raise SystemExit(f"alpha68 overlay missing {required}")

approved_backgrounds = {
    "standard_gacha.png": "694aadcaca9a8dcaace9c26a9e43cf41e056203855cd380670d0b7c208f4d98d",
    "emi_gacha.png": "4c61d6a00e324a82d060b8c7b9a3abe40daa437fda8d8efb5a4eaf45b973e216",
}
background_root = candidate / "src/main/resources/assets/emipokemon/textures/gui/gacha"
for name, expected in approved_backgrounds.items():
    actual = hashlib.sha256((background_root / name).read_bytes()).hexdigest()
    if actual != expected:
        raise SystemExit(f"approved {name} changed unexpectedly: {actual}")

if (background_root / "reveal_sheet.png").exists():
    raise SystemExit("rejected gacha video sheet is still packaged")

for tool in ("emi_sword", "emi_pickaxe", "emi_axe", "emi_shovel", "emi_hoe"):
    for relative in (
        f"src/main/resources/assets/emipokemon/models/item/{tool}.json",
        f"src/main/resources/assets/emipokemon/textures/item/{tool}.png",
    ):
        if not (candidate / relative).is_file():
            raise SystemExit(f"missing Emi tool asset: {relative}")

machine = (candidate / "src/main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java").read_text()
renderer = (candidate / "src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java").read_text()
if "ParticleTypes.HEART" not in machine or "EMI_PINK" not in machine:
    raise SystemExit("Emi seasonal effects are incomplete")
if "POKÉMON DE TEMPORADA" not in renderer or "drawProfilePokemon" not in renderer:
    raise SystemExit("seasonal Pokémon billboard renderer is incomplete")

print("alpha68 overlay, approved gacha backgrounds and Emi tool assets validated")
