from pathlib import Path
import hashlib
import json

root = Path.cwd()

def text(path):
    return (root / path).read_text(encoding="utf-8")

def sha(path):
    return hashlib.sha256((root / path).read_bytes()).hexdigest()

screen = text("src/client/java/com/emipokemon/client/gacha/GachaScreen.java")
world = text("src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java")
banner = text("src/main/java/com/emipokemon/gacha/banner/BannerDefinition.java")
block_entity = text("src/main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java")
identity = text("src/main/java/com/emipokemon/Emipokemon.java")
claw_path = root / "src/main/resources/assets/emipokemon/geo/casino_claw.geo.json"
claw = json.loads(claw_path.read_text(encoding="utf-8"))

assert "mod_version=0.4.0-alpha.70" in text("gradle.properties")
assert 'public static final String VERSION = "0.4.0-alpha.70";' in identity
assert "0.4.0-alpha.69" not in identity

# Keep the safe clipping introduced in alpha69, but restore readable portraits.
assert "enableScissor" in screen
assert "top + ry(126), rx(82)" in screen
assert "singlePortraitBottomY()), rx(190)" in screen
assert "RESULT_STAGGER_MS = 135L" in screen

# Real migration of old default JSONs to a 1% combined legendary/mythical baseline.
assert "public int schemaVersion = 1;" in banner
assert 'putWeight(GachaTier.LEGENDARY, 0.8)' in banner
assert 'putWeight(GachaTier.MYTHICAL, 0.2)' in banner
assert "migrateLegacyDefaultTierWeights" in banner
assert 'nearly(tierWeights.get("LEGENDARY"), 4.0)' in banner
assert 'nearly(tierWeights.get("MYTHICAL"), 1.0)' in banner

# Do not deserialize intentionally incomplete Pokemon NBT; resolve/set species instead.
assert ".loadFromNBT(" not in world
assert "NbtCompound" not in world
assert "com.cobblemon.mod.common.api.pokemon.PokemonSpecies" in world
assert "getByIdentifier" in world
assert "setSpecies" in world
assert "setPokemon" in world
assert world.index("drawTextLine") < world.index("renderWorldPokemon(machine")

# Empty machine spotlight data gets repaired immediately, not only at the 5-second cadence.
assert "machine.featuredSpeciesId == null || machine.featuredSpeciesId.isBlank()" in block_entity
assert "staggered % 100L == 0L" in block_entity

# Physical claw fix must be geometry only and include the side shell/bridges.
bones = claw["minecraft:geometry"][0]["bones"]
shell = next((bone for bone in bones if bone.get("name") == "alpha70_side_shell"), None)
assert shell is not None
assert len(shell.get("cubes", [])) == 8
assert any(cube.get("size") == [1.0, 7.2, 12.0] for cube in shell["cubes"])
assert any(cube.get("size") == [1.0, 7.8, 12.0] for cube in shell["cubes"])
assert sum(1 for cube in shell["cubes"] if cube.get("size", [0,0,0])[1] == 20.7) == 4

# Immutable approved assets.
assert sha("src/main/resources/assets/emipokemon/textures/gui/gacha/standard_gacha.png") == "694aadcaca9a8dcaace9c26a9e43cf41e056203855cd380670d0b7c208f4d98d"
assert sha("src/main/resources/assets/emipokemon/textures/gui/gacha/emi_gacha.png") == "4c61d6a00e324a82d060b8c7b9a3abe40daa437fda8d8efb5a4eaf45b973e216"
assert sha("src/main/resources/assets/emipokemon/textures/block/standard_gacha_machine.png") == "42d060134c7809f2617bcf1aa7a06f02750908eb616fcbbfb3b837dbf98e30cd"
assert sha("src/main/resources/assets/emipokemon/textures/block/emi_gacha_machine.png") == "4d0d76caca51f7d167d3197030d7074e9ac2b7e1ade015c5a01c462ef3e4aea0"
assert not (root / "src/main/resources/assets/emipokemon/textures/gui/gacha/reveal_sheet.png").exists()
print("alpha70 regression preflight passed")
