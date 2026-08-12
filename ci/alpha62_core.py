from pathlib import Path
import shutil

root = Path(".")
payload = Path(__file__).resolve().parent / "alpha62_source"

if not (root / "src").exists():
    raise AssertionError("alpha.62 must be applied after alpha.61 reconstruction")

shutil.rmtree(root / "src")
shutil.copytree(payload / "src", root / "src")
shutil.copy2(payload / "build.gradle", root / "build.gradle")
shutil.copy2(payload / "gradle.properties", root / "gradle.properties")
print("alpha.62 claw tickets, Pokédoll machine, fair payouts and Pokémon wager escrow applied")
