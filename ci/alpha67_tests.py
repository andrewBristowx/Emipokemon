from pathlib import Path
from zipfile import ZipFile
import hashlib

root = Path(__file__).resolve().parents[1]
candidate = Path.cwd()
overlay = root / "ci" / "alpha67_overlay.zip"

with ZipFile(overlay) as archive:
    names = set(archive.namelist())
    for required in (
        "src/client/java/com/emipokemon/client/gacha/GachaScreen.java",
        "src/client/java/com/emipokemon/client/render/PokemonPortraitRenderer.java",
        "src/main/java/com/emipokemon/progress/RankGroups.java",
        "src/main/resources/assets/emipokemon/geo/casino_claw.geo.json",
    ):
        if required not in names:
            raise SystemExit(f"alpha67 overlay missing {required}")

# Exact physical texture rollback: compare all eleven files to reconstructed alpha.65.
expected = {
    "casino_blackjack.png": "92bd891fdc5e5c32c0e585cf9febad71d18570d55cf59726779b6f46c3b5e62e",
    "casino_chip_exchange.png": "7b15e8b9d4f3c23030820b56b3a4c915183e4deaa5540247c5ada7b7e409f9fd",
    "casino_claw.png": "a4ed95886b8c752c20eed52e2d492b91da0e8ebd2c55605fd6a48daf8c8aee46",
    "casino_dice.png": "57fded34390c3b2af7293af398029e6308af2f38d35f7a99c58174b4dca52b85",
    "casino_pokemon_flip.png": "df80c584a1a1ed3e4c680dbb62ebf1627985dfce4a4d19db2f0871da6cebb469",
    "casino_poker.png": "df80c584a1a1ed3e4c680dbb62ebf1627985dfce4a4d19db2f0871da6cebb469",
    "casino_roulette.png": "4fb4f9ebeec351f5f7dc035936c10bec97cf9d99dab27af1582ab6350d35c468",
    "casino_slot.png": "99de3521ac02b55d729bf93291b2c4532932dc307a2afdaf15acab585a8651d5",
    "casino_ticket_exchange.png": "a4ed95886b8c752c20eed52e2d492b91da0e8ebd2c55605fd6a48daf8c8aee46",
    "emi_gacha_machine.png": "4d0d76caca51f7d167d3197030d7074e9ac2b7e1ade015c5a01c462ef3e4aea0",
    "standard_gacha_machine.png": "42d060134c7809f2617bcf1aa7a06f02750908eb616fcbbfb3b837dbf98e30cd",
}
base = candidate / "src/main/resources/assets/emipokemon/textures/block"
for name, digest in expected.items():
    actual = hashlib.sha256((base / name).read_bytes()).hexdigest()
    if actual != digest:
        raise SystemExit(f"{name} is not the exact restored alpha65 texture: {actual}")

if (candidate / "src/main/resources/assets/emipokemon/textures/gui/gacha/reveal_sheet.png").exists():
    raise SystemExit("rejected gacha video sheet is still packaged")

print("alpha67 overlay and exact physical texture rollback validated")
