from pathlib import Path

root = Path(".")
read = lambda path: (root / path).read_text(encoding="utf-8")
service = read("src/main/java/com/emipokemon/casino/CasinoService.java")
wager = read("src/main/java/com/emipokemon/casino/PokemonWagerService.java")
tables = read("src/main/java/com/emipokemon/casino/CasinoTableService.java")

assert "mod_version=0.4.0-alpha.62" in read("gradle.properties")
assert "CLAW_TICKET" in service and "clawTicketPrice" in service
assert "pokeblocks:pokedoll_eevee" in read("src/main/java/com/emipokemon/config/EmipokemonConfig.java")
assert "writeEscrow(escrow)" in wager and "firstParty.remove(first)" in wager
assert wager.index("writeEscrow(escrow)") < wager.index("firstParty.remove(first)")
assert "while (firstWins < 2 && secondWins < 2)" in wager
assert "restoreIfMissing" in wager and "deliverIfMissing" in wager
assert "Esta ronda usa una entrada única" in tables
for asset in ("casino_claw.png", "casino_pokemon_flip.png"):
    path = root / "src/main/resources/assets/emipokemon/textures/gui/casino/finished" / asset
    assert path.exists() and path.stat().st_size > 100_000, asset
print("alpha.62 ticket separation, real Pokeblocks IDs, payout policy, escrow and visual assets passed")
