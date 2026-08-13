from pathlib import Path


root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")
client = (root / "src/client/java/com/emipokemon/client/casino/CasinoClient.java").read_text(encoding="utf-8")
server = (root / "src/main/java/com/emipokemon/casino/CasinoTableService.java").read_text(encoding="utf-8")

for marker in (
    "PresentationState", "presentationState()", "updateCasinoSounds", "playTimedTicks",
    "PositionedSoundInstance.master", "ITEM_BOOK_PAGE_TURN", "BLOCK_DISPENSER_LAUNCH",
    "ENTITY_EXPERIENCE_ORB_PICKUP", "diceResultSource()", "elapsed < 2600L",
    "context.getMatrices().scale(pulse, pulse, 1.0F)",
):
    assert marker in screen, marker
assert "casino.presentationState()" in client
assert "new CasinoScreen(parent, json, previousAmount, presentation)" in client
assert 'result(session, "Dados compartidos: " + first + " + " + second' in server
assert "mod_version=0.4.0-alpha.61" in (root / "gradle.properties").read_text(encoding="utf-8")
print("alpha.61 sound rate limiting, persistent timelines and authoritative dice-result checks passed")
