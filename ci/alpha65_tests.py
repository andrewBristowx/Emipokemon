from pathlib import Path
import json
import struct

root = Path.cwd()

def text(path: str) -> str:
    return (root / path).read_text(encoding="utf-8")

assert "mod_version=0.4.0-alpha.65" in text("gradle.properties")
assert "0.4.0-alpha.65" in text("src/main/java/com/emipokemon/Emipokemon.java")

portrait = text("src/client/java/com/emipokemon/client/render/PokemonPortraitRenderer.java")
assert "blockbench.FloatingState" in portrait
assert "getDeclaredConstructor()" in portrait
assert "getParameterCount() == 16" in portrait
assert "1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F" in portrait

pass_service = text("src/main/java/com/emipokemon/rewards/BattlePassService.java")
assert "legacy_roll_materialized" in pass_service
assert "emi_special_banner_ticket" in pass_service
assert "onJobAction" in pass_service
assert "jobActionPassXpPerMinute" in pass_service

catalog = text("src/main/java/com/emipokemon/progress/QuestCatalog.java")
assert catalog.count("tutorial(") == 12  # helper plus eleven tutorial entries
assert "minecraft:dimension:the_nether" in catalog
assert "battlePassXp" in text("src/main/java/com/emipokemon/progress/QuestDefinition.java")

journal = text("src/client/java/com/emipokemon/client/progress/QuestJournalScreen.java")
for label in ("Guía", "Minecraft", "Pokémon", "Aventura", "Trabajos"):
    assert label in journal
assert "XP de pase" in journal

for name in ("michicoin_icon.png", "random_pokemon_icon.png"):
    data = (root / "src/main/resources/assets/emipokemon/textures/item" / name).read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    width, height = struct.unpack(">II", data[16:24])
    assert (width, height) == (64, 64)

for model in (
    "blackjack", "chip_exchange", "claw", "dice", "pokemon_flip",
    "poker", "roulette", "slot", "ticket_exchange"
):
    geometry = json.loads(text(f"src/main/resources/assets/emipokemon/geo/casino_{model}.geo.json"))
    bones = geometry["minecraft:geometry"][0]["bones"]
    cubes = sum(len(bone.get("cubes", [])) for bone in bones)
    assert cubes >= 46, (model, cubes)
    assert any(bone.get("name") == "alpha65_ornaments" for bone in bones)

print("alpha65 static checks passed")
