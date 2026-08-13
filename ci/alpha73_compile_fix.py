#!/usr/bin/env python3
from pathlib import Path

path = Path.cwd() / "src/main/java/com/emipokemon/gacha/machine/GachaMachineLabelService.java"
text = path.read_text(encoding="utf-8")
old = "import java.util.List;\n\n"
text = text.replace(old, "", 1)
old_decl = "        List<DisplayEntity.TextDisplayEntity> labels = world.getEntitiesByType(\n"
new_decl = "        var labels = world.getEntitiesByType(\n"
if old_decl not in text:
    raise SystemExit("alpha73 compile-fix target declaration missing")
text = text.replace(old_decl, new_decl, 1)
path.write_text(text, encoding="utf-8")
print("alpha.73 TextDisplayEntity wildcard compile fix applied")
