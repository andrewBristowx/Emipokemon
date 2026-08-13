from pathlib import Path
import hashlib
import json

root = Path.cwd()

def text(path):
    return (root / path).read_text(encoding="utf-8")

def sha(path):
    return hashlib.sha256((root / path).read_bytes()).hexdigest()

portrait = text("src/client/java/com/emipokemon/client/render/PokemonPortraitRenderer.java")
seasonal = text("src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java")
networking = text("src/main/java/com/emipokemon/gacha/GachaNetworking.java")
client = text("src/client/java/com/emipokemon/client/gacha/GachaClient.java")
machine = text("src/main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java")
claw = json.loads(text("src/main/resources/assets/emipokemon/geo/casino_claw.geo.json"))

assert "mod_version=0.4.0-alpha.71" in text("gradle.properties")
assert 'VERSION = "0.4.0-alpha.71"' in text("src/main/java/com/emipokemon/Emipokemon.java")

assert "com.cobblemon.mod.common.api.gui.GuiUtilsKt" in portrait
assert 'method.getName().equals("drawProfile")' in portrait
assert "FloatingState" in portrait
assert 'Class.forName("com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt")' not in portrait
assert 'method.getName().equals("drawProfilePokemon")' not in portrait

assert "SeasonalDisplayPayload" in networking
assert "gacha_seasonal_display" in networking
assert "syncSeasonalDisplay" in networking
assert "registerGlobalReceiver(SeasonalDisplayPayload.ID" in client
assert "applyClientFeaturedPokemon" in client
assert "staggered % 40L == 0L" in machine
assert "GachaNetworking.syncSeasonalDisplay(serverWorld, this)" in machine

assert "4.18D" in seasonal
assert "2.78D + bob" in seasonal
assert "Sincronizando…" in seasonal
assert seasonal.index("drawTextLine") < seasonal.index("if (species.isBlank()) return")

bones = claw["minecraft:geometry"][0]["bones"]
seal = next((bone for bone in bones if bone.get("name") == "alpha71_sealed_joints"), None)
assert seal is not None
assert len(seal.get("cubes", [])) == 10
assert any(cube.get("size") == [16.4, 2.25, 1.35] for cube in seal["cubes"])
assert any(cube.get("size") == [16.1, 1.55, 12.7] for cube in seal["cubes"])
assert sum(1 for cube in seal["cubes"] if cube.get("size", [0,0,0])[1] == 22.9) == 4

assert sha("src/main/resources/assets/emipokemon/textures/gui/gacha/standard_gacha.png") == "694aadcaca9a8dcaace9c26a9e43cf41e056203855cd380670d0b7c208f4d98d"
assert sha("src/main/resources/assets/emipokemon/textures/gui/gacha/emi_gacha.png") == "4c61d6a00e324a82d060b8c7b9a3abe40daa437fda8d8efb5a4eaf45b973e216"
assert sha("src/main/resources/assets/emipokemon/textures/block/standard_gacha_machine.png") == "42d060134c7809f2617bcf1aa7a06f02750908eb616fcbbfb3b837dbf98e30cd"
assert sha("src/main/resources/assets/emipokemon/textures/block/emi_gacha_machine.png") == "4d0d76caca51f7d167d3197030d7074e9ac2b7e1ade015c5a01c462ef3e4aea0"
assert not (root / "src/main/resources/assets/emipokemon/textures/gui/gacha/reveal_sheet.png").exists()
print("alpha71 regression preflight passed")
