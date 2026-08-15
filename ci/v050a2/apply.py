from pathlib import Path
import base64
import hashlib
import io
import zipfile

root = Path(__file__).resolve().parent
payload = "".join((root / f"payload.{i:02d}").read_text().strip() for i in range(8))
data = base64.b64decode(payload)
# This is the exact encoded payload currently stored in GitHub. The content
# manifest below is the authoritative validation so ZIP metadata changes cannot
# accidentally invalidate an otherwise byte-identical source patch.
expected_payload = "81c653e9d706ecf79c9870d2ba11b7df30b5953ce57f29ca4f37e776e561d1ab"
actual_payload = hashlib.sha256(data).hexdigest()
if actual_payload != expected_payload:
    raise SystemExit(f"alpha.2 payload hash mismatch: {actual_payload} != {expected_payload}")

expected = {
    "CHANGELOG-0.5.0-alpha.2.md": "84370daa222a1184cfe63c7b970d2faf726b1ab1e63c7168c917780713a4c1d0",
    "GUIA-PRUEBAS-0.5.0-alpha.2.md": "51993a848e300a76661b5096b973a108f4cba73317059b01cf3a3f1e0b63a0bd",
    "gradle.properties": "5e3b62d7245d620342ada83267d93c950e3fddb187478f618ef293dd60bd666f",
    "src/client/java/com/emipokemon/client/npc/NpcDialogueScreen.java": "ff6030356436752b35a2035e2d8d03dc3a0daf774cf907b9cea6e36ff361f2e1",
    "src/client/java/com/emipokemon/client/render/CustomNpcRenderer.java": "fcf168b57bafb0109d0939a8b77680d6a6d3fb150d622458985edfa8cc4fca0f",
    "src/main/java/com/emipokemon/Emipokemon.java": "1932d70f85af6692333a7db9467e21911aecf7c4883a73399976f93cae7bfbe4",
    "src/main/java/com/emipokemon/challenge/ChallengeCatalog.java": "fcf401afbe32945bb78303653ddc0656f67315ab46d82340bdf3e06a46ef8555",
    "src/main/java/com/emipokemon/challenge/ChallengeCommands.java": "6e7b032e04527d9b930c117a6462ede23d37c022238d80f86ace5410cf450222",
    "src/main/java/com/emipokemon/challenge/ChallengeProfile.java": "9f29c55720f58dfdadd90ab77a4f2c22ae6086cc393abc07b0e0667583022117",
    "src/main/java/com/emipokemon/challenge/ChallengeService.java": "83891a23bbf212f4a7c576fe8127a09d80c8163d79beec8a3179dd43766c7808",
    "src/main/java/com/emipokemon/data/PlayerData.java": "d804bc29121a40f89f03aa70389e1e548edf9d76daa279b71f37c34f0ace7549",
    "src/main/java/com/emipokemon/npc/NpcBattleService.java": "b95e8daf5dc3425147f15bae5496c571ac0cb09ea456c228b4b67d9402751eda",
    "src/main/java/com/emipokemon/npc/NpcNetworking.java": "341701ba7850ba701bef5ed812c9d6206f0902029ee4b8ed6fc932899b81de63",
    "src/main/java/com/emipokemon/progress/ProgressionService.java": "97153cb153347c3fd5a2d4ac965a88e8c9a58a4f3d9cfe55e8ae1ea98ab6c822",
    "src/main/java/com/emipokemon/progress/QuestCatalog.java": "7fd0fc4cdb73baec20af1336d8bb6cbbc42c61d8eed9120e0307a1469ab54fa7",
    "src/main/resources/assets/emipokemon/textures/entity/professor_emi.png": "1b5358e5ae0d05424bd63219cdfeab38712cf74218d228bc5516d7763f3046cc",
    "src/test/java/com/emipokemon/challenge/ChallengeCatalogTest.java": "e5a65dca44d7831091ecc90b668c81b117d9275216a33a2d469016c62ea9e568",
    "src/test/java/com/emipokemon/data/PlayerDataMigrationTest.java": "3efc9de8020049d691917ac5f7bd3cf4c20576a092d1b5bd906b2b45aadfe852",
    "src/test/java/com/emipokemon/progress/QuestCatalogTest.java": "0d8cc15341ec904ba81f61410359f0242f6fa60d0988df48556e5a654504a25a",
    "src/test/java/com/emipokemon/v050a2/Version050Alpha2RegressionTest.java": "ff8d9c56874c31d6b94e71df4891047aee2e859e24fd310d7de9b631a04b9e73",
}

with zipfile.ZipFile(io.BytesIO(data)) as archive:
    names = archive.namelist()
    if set(names) != set(expected):
        raise SystemExit(f"alpha.2 payload file set mismatch: {set(names) ^ set(expected)}")
    for name in names:
        content = archive.read(name)  # also validates the ZIP entry CRC
        actual = hashlib.sha256(content).hexdigest()
        if actual != expected[name]:
            raise SystemExit(f"alpha.2 file hash mismatch for {name}: {actual} != {expected[name]}")
        target = Path(name)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)

print(f"Applied and verified Emipokemon 0.5.0-alpha.2 patch ({len(names)} files).")
