from pathlib import Path
import struct

root = Path(".")
read = lambda path: (root / path).read_text(encoding="utf-8")

assert "mod_version=0.4.0-alpha.64" in read("gradle.properties")
assert "0.4.0-alpha.64" in read("src/main/java/com/emipokemon/Emipokemon.java")
assert "dataVersion = 6" in read("src/main/java/com/emipokemon/data/PlayerData.java")

daily = read("src/main/java/com/emipokemon/rewards/DailyRewardService.java")
assert daily.index("writeOperation(operation)") < daily.index("dataManager.saveNowChecked(player.getUuid())")
assert daily.index("dataManager.saveNowChecked(player.getUuid())") < daily.index("deliverExternal(player, operation)")
assert "ZoneId.of(settings().timeZone)" in daily and "recoverForPlayer" in daily

config = read("src/main/java/com/emipokemon/config/EmipokemonConfig.java")
for requested in ("minecraft:diamond", "cobblemon:ultra_ball", "cobblemon:rare_candy", "MICHICOINS", "STANDARD_ROLLS", "EMI_ROLLS", "POKEMON"):
    assert requested in config, requested
for rule in ("freeRewardEveryLevels = 4", "freeEmiRolls = 1", "premiumFirstLevelEmiRolls = 10", "premiumEmiRolls = 2"):
    assert rule in config, rule

pass_service = read("src/main/java/com/emipokemon/rewards/BattlePassService.java")
assert "totalXpForLevel" in pass_service and "claimedPremium" in pass_service
assert "captureXpEventsPerMinute" in pass_service and "level == 1" in pass_service

screen = read("src/client/java/com/emipokemon/client/casino/CasinoScreen.java")
portrait = read("src/client/java/com/emipokemon/client/render/PokemonPortraitRenderer.java")
assert "PokemonPortraitRenderer.draw" in screen
assert 'getParameterCount() == 13' in portrait and 'getParameterCount() == 16' in portrait
assert '!"claw".equals(game) && !"pokemon_flip".equals(game)' in screen

for asset in (
    "src/main/resources/assets/emipokemon/textures/gui/daily_reward.png",
    "src/main/resources/assets/emipokemon/textures/gui/battle_pass.png",
):
    path = root / asset
    assert path.stat().st_size > 500_000, asset
    with path.open("rb") as source:
        header = source.read(24)
    assert header[:8] == b"\x89PNG\r\n\x1a\n", asset
    assert struct.unpack(">II", header[16:24]) == (1536, 1024), asset

print("alpha.64 persistence order, reward rules, portraits and generated assets passed")
