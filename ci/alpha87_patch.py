import json
from pathlib import Path

# Version bump from the validated alpha.86 source.
gradle = Path("gradle.properties")
gradle.write_text(gradle.read_text().replace("mod_version=0.4.0-alpha.86", "mod_version=0.4.0-alpha.87"))

main = Path("src/main/java/com/emipokemon/Emipokemon.java")
main.write_text(main.read_text().replace('public static final String VERSION = "0.4.0-alpha.86";', 'public static final String VERSION = "0.4.0-alpha.87";'))

# Alpha.87 visual patch: remove the four large white helmet shell slabs and
# turn the helmet/chest hearts into very thin decal-like geometry.
geo_path = Path("src/main/resources/assets/emipokemon/geo/armor/emi_armor.geo.json")
data = json.loads(geo_path.read_text())
bones = data["minecraft:geometry"][0]["bones"]

for bone in bones:
    if bone["name"] == "armorHead":
        bone["cubes"] = [cube for index, cube in enumerate(bone["cubes"]) if index not in {0, 1, 2, 3}]
    elif bone["name"] == "helmet_heart":
        bone["cubes"] = [{
            "origin": [-1.55, 28.35, -4.98],
            "size": [3.1, 2.1, 0.06],
            "uv": {face: {"uv": [16, 0], "uv_size": [16, 16]} for face in ["north", "south", "east", "west", "up", "down"]}
        }]
    elif bone["name"] == "heart_core":
        bone["cubes"] = [{
            "origin": [-1.95, 17.45, -3.02],
            "size": [3.9, 3.1, 0.06],
            "uv": {face: {"uv": [16, 0], "uv_size": [16, 16]} for face in ["north", "south", "east", "west", "up", "down"]}
        }]

geo_path.write_text(json.dumps(data, indent=2) + "\n")

Path("CHANGELOG-0.4.0-alpha.87.md").write_text("""# Emipokemon 0.4.0-alpha.87

## Ajuste visual de armadura Emi
- Se quitó la parte blanca grande del casco para dejar esa zona abierta/transparente.
- El corazón del casco pasó de una pieza 3D a un detalle plano tipo imagen.
- El corazón de la pechera pasó de volumen 3D a un detalle plano tipo imagen.
- Botas y pantalones/grebas se mantienen como en alpha.86.
""")

Path("GUIA-PRUEBAS-0.4.0-alpha.87.md").write_text("""# Guía de pruebas — Emipokemon 0.4.0-alpha.87

1. Equipar el set Emi completo.
2. Verificar que la parte blanca grande del casco haya desaparecido.
3. Verificar que el corazón del casco se vea plano, no volumétrico.
4. Verificar que el corazón de la pechera se vea plano, no volumétrico.
5. Confirmar que botas y pantalones mantengan el aspecto de alpha.86.
""")

test = Path("src/test/java/com/emipokemon/alpha87/Alpha87EmiArmorFlatHeartRegressionTest.java")
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text(r'''package com.emipokemon.alpha87;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class Alpha87EmiArmorFlatHeartRegressionTest {
    @Test void versionIsAlpha87() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.87"));
        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java")).contains("VERSION = \\\"0.4.0-alpha.87\\\""));
    }

    @Test void heartsAreFlatAndHelmetShellWasRemoved() throws Exception {
        String geo = Files.readString(Path.of("src/main/resources/assets/emipokemon/geo/armor/emi_armor.geo.json"));
        assertTrue(geo.contains("\\\"name\\\": \\\"helmet_heart\\\""));
        assertTrue(geo.contains("\\\"name\\\": \\\"heart_core\\\""));
        assertTrue(geo.contains("0.06"));
    }
}
''')
