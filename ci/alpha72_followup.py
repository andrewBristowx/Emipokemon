#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
renderer = root / "src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java"
s = renderer.read_text(encoding="utf-8")
if "import org.joml.Quaternionf;" not in s:
    s = s.replace("import org.joml.Matrix4f;\n", "import org.joml.Matrix4f;\nimport org.joml.Quaternionf;\n", 1)
s = s.replace(
    "matrices.multiply(client.getEntityRenderDispatcher().getRotation());",
    "matrices.multiply(new Quaternionf(client.getEntityRenderDispatcher().getRotation()));",
    1,
)
renderer.write_text(s, encoding="utf-8")

# Keep the runtime identity in sync with gradle.properties.
core = root / "src/main/java/com/emipokemon/Emipokemon.java"
core_text = core.read_text(encoding="utf-8")
if "0.4.0-alpha.71" not in core_text:
    raise SystemExit("alpha72: expected alpha.71 runtime identity before patch")
core.write_text(core_text.replace("0.4.0-alpha.71", "0.4.0-alpha.72"), encoding="utf-8")

# Historical regression suites intentionally pin the candidate version. Advance those pins to alpha.72.
for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    if "0.4.0-alpha.71" in text:
        test.write_text(text.replace("0.4.0-alpha.71", "0.4.0-alpha.72"), encoding="utf-8")

if "new Quaternionf(client.getEntityRenderDispatcher().getRotation())" not in renderer.read_text(encoding="utf-8"):
    raise SystemExit("alpha72: camera-facing billboard regression guard missing")
if "0.4.0-alpha.72" not in core.read_text(encoding="utf-8"):
    raise SystemExit("alpha72: runtime identity was not advanced")
print("alpha.72 regression pins and runtime identity advanced")
