from pathlib import Path
import hashlib

data = bytes.fromhex(
    "89504e470d0a1a0a0000000d49484452000000100000001008060000001ff3ff61"
    "0000009349444154789c63601868c0488ae2237613ffc3d83687f249d2cb70c46e"
    "e2ffff5ffec3b18a98d67fc2bad0343f38f5eeff8353effe7f7af0136e0013319a"
    "f51665303cbcf69ee1dedefb24b91a0eacd45cfeef6b3f8b613bd19abfecf0f8af"
    "22a6459966060606069235bf089a4b9a8dc89acb625ae8acd94acde5bf959acbff"
    "89054bc9b71939c0c8022a625a9419400e00002cdc702de70976ea000000004945"
    "4e44ae426082"
)
expected = "f528d40ff3f1cd947b5a6e2b8bbea9e5a0b2f4bbbec17b4eaaabf0a5d484399a"
actual = hashlib.sha256(data).hexdigest()
if actual != expected:
    raise SystemExit(f"approved shovel hash mismatch: {actual} != {expected}")
Path("src/main/resources/assets/emipokemon/textures/item/emi_shovel.png").write_bytes(data)
print("Restored exact approved alpha.98 shovel texture.")
