from pathlib import Path
import hashlib

root = Path.cwd()

def text(path):
    return (root / path).read_text(encoding="utf-8")

def sha(path):
    return hashlib.sha256((root / path).read_bytes()).hexdigest()

screen = text("src/client/java/com/emipokemon/client/gacha/GachaScreen.java")
world = text("src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java")
rotation = text("src/main/java/com/emipokemon/gacha/banner/FeaturedRotationService.java")
service = text("src/main/java/com/emipokemon/gacha/GachaService.java")
machine_renderer = text("src/client/java/com/emipokemon/client/render/GachaMachineRenderer.java")

assert "RESULT_STAGGER_MS = 135L" in screen
assert "enableScissor" in screen and "PositionedSoundInstance.master" in screen
assert "drawProfilePokemon" not in world
assert "CobblemonEntities.POKEMON.create" in world
assert "12L * 60L * 60L * 1000L" in rotation
assert "GachaTier.LEGENDARY" in rotation
assert "EMI_FEATURED_MULTIPLIER" in service
assert "getEntityTranslucent(texture)" in machine_renderer
assert "getEntityTranslucentCull(texture)" not in machine_renderer
assert sha("src/main/resources/assets/emipokemon/textures/gui/gacha/standard_gacha.png") == "694aadcaca9a8dcaace9c26a9e43cf41e056203855cd380670d0b7c208f4d98d"
assert sha("src/main/resources/assets/emipokemon/textures/gui/gacha/emi_gacha.png") == "4c61d6a00e324a82d060b8c7b9a3abe40daa437fda8d8efb5a4eaf45b973e216"
assert sha("src/main/resources/assets/emipokemon/textures/block/standard_gacha_machine.png") == "42d060134c7809f2617bcf1aa7a06f02750908eb616fcbbfb3b837dbf98e30cd"
assert sha("src/main/resources/assets/emipokemon/textures/block/emi_gacha_machine.png") == "4d0d76caca51f7d167d3197030d7074e9ac2b7e1ade015c5a01c462ef3e4aea0"
assert not (root / "src/main/resources/assets/emipokemon/textures/gui/gacha/reveal_sheet.png").exists()
print("alpha69 regression preflight passed")
