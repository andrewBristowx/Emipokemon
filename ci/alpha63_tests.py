from pathlib import Path

root = Path(".")
read = lambda path: (root / path).read_text(encoding="utf-8")

assert "mod_version=0.4.0-alpha.63" in read("gradle.properties")
assert "0.4.0-alpha.63" in read("src/main/java/com/emipokemon/Emipokemon.java")
claw = read("src/main/java/com/emipokemon/casino/ClawGameService.java")
screen = read("src/client/java/com/emipokemon/client/casino/CasinoScreen.java")
wager = read("src/main/java/com/emipokemon/casino/PokemonWagerService.java")
assert "LANE_COUNT = 5" in claw
assert 'operation.status = "PREPARED"' in claw
assert claw.index("writeOperation(operation)") < claw.index("removeOne(player, ModRegistries.CLAW_TICKET)")
assert "deliver(player, new ItemStack(ModRegistries.CLAW_TICKET))" in claw
assert claw.index("writeOperation(operation)") < claw.index("deliver(player, new ItemStack(prize))")
assert "id.getPath().startsWith(\"pokedoll_\")" in claw
assert "drawInteractiveClaw(context)" in screen and "context.drawItem(stack, 0, 0)" in screen
assert "drawPokemonFlipSelections(context)" in screen and "drawCobblemonPortrait" in screen
assert "first.getUuid().equals(second.getUuid())" in wager
assert "pokemon_flip:result_committed" in wager and "pokemon_flip:delivered" in wager
for asset in (
    "src/main/resources/assets/emipokemon/textures/gui/casino/finished/casino_claw.png",
    "src/main/resources/assets/emipokemon/textures/item/claw_ticket.png",
    "src/main/resources/assets/emipokemon/geo/casino_claw.geo.json",
    "src/main/resources/assets/emipokemon/geo/casino_pokemon_flip.geo.json",
):
    assert (root / asset).exists() and (root / asset).stat().st_size > 500, asset
print("alpha.63 claw gameplay, Pokémon display, persistence order and visual assets passed")
