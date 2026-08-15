#!/usr/bin/env python3
from pathlib import Path

ROOT = Path.cwd()

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

def write(path, content):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")

def require(condition, message):
    if not condition:
        raise SystemExit(message)

require("mod_version=0.4.0-alpha.98" in read("gradle.properties"), "requires exact alpha.98 base")
require('VERSION = "0.4.0-alpha.98"' in read("src/main/java/com/emipokemon/Emipokemon.java"), "alpha.98 runtime marker missing")
require('matrices.translate(0.5D, 4.50D + bob, 0.5D);' in read("src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java"), "alpha.98 treasure sword transform missing")

# Version.
write("gradle.properties", read("gradle.properties").replace("mod_version=0.4.0-alpha.98", "mod_version=0.5.0-alpha.1"))
write("src/main/java/com/emipokemon/Emipokemon.java", read("src/main/java/com/emipokemon/Emipokemon.java").replace('VERSION = "0.4.0-alpha.98"', 'VERSION = "0.5.0-alpha.1"'))

# Tesoros de Emi: move only the floating decorative sword. The approved handheld/inventory tool model is untouched.
p = "src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java"
s = read(p)
s = s.replace('matrices.translate(0.5D, 4.50D + bob, 0.5D);', 'matrices.translate(0.5D, 5.18D + bob, 0.5D);')
write(p, s)

# Battle pass: preserve special Emi-banner tickets and add Treasure tickets as recurring rewards.
p = "src/main/java/com/emipokemon/rewards/BattlePassService.java"
s = read(p)
require('default -> BattlePassReward.item("minecraft:gold_ingot", 6, "Lingotes de oro");' in s, "premium pass rotation changed")
require('default -> BattlePassReward.item("minecraft:redstone", 8, "Polvo de redstone");' in s, "free pass rotation changed")
s = s.replace('default -> BattlePassReward.item("minecraft:gold_ingot", 6, "Lingotes de oro");',
              'default -> BattlePassReward.item("emipokemon:treasure_gacha_ticket", 2, "Tickets de Tesoros de Emi");')
s = s.replace('default -> BattlePassReward.item("minecraft:redstone", 8, "Polvo de redstone");',
              'default -> BattlePassReward.item("emipokemon:treasure_gacha_ticket", 1, "Ticket de Tesoros de Emi");')
write(p, s)

p = "src/client/java/com/emipokemon/client/rewards/BattlePassScreen.java"
s = read(p)
s = s.replace('Todos los niveles dan premio · Cada 4 niveles: tirada de Emi',
              'Todos los niveles dan premio · Cada 4 niveles: tirada de Emi · Incluye Tickets de Tesoros')
write(p, s)

# Config v10: add the Treasure ticket to fresh daily reward pools and migrate old configs without deleting custom rewards.
p = "src/main/java/com/emipokemon/config/EmipokemonConfig.java"
s = read(p)
require('public int configVersion = 9;' in s, "alpha.98 config version changed")
s = s.replace('public int configVersion = 9;', 'public int configVersion = 10;', 1)
s = s.replace('    public void normalize() {\n        if (hub == null) {',
              '    public void normalize() {\n        int previousConfigVersion = configVersion;\n        if (hub == null) {', 1)
s = s.replace('        migrateLegacyRankGroups();\n        configVersion = 9;\n    }',
'''        migrateLegacyRankGroups();
        if (previousConfigVersion < 10) migrateTreasureTicketRewards();
        configVersion = 10;
    }

    /** 0.5.0 adds the Tesoros de Emi ticket without deleting customized daily rewards. */
    private void migrateTreasureTicketRewards() {
        boolean present = dailyRewards.rewards.stream().anyMatch(reward -> reward != null
                && "ITEM".equalsIgnoreCase(reward.type)
                && "emipokemon:treasure_gacha_ticket".equalsIgnoreCase(reward.value));
        if (!present) {
            dailyRewards.rewards.add(new DailyRewardEntry(
                    "treasure_ticket", "ITEM", "emipokemon:treasure_gacha_ticket", 1, 6));
        }
    }''', 1)
old_daily = '''                    new DailyRewardEntry("emi_roll", "EMI_ROLLS", "", 1, 5),
                    new DailyRewardEntry("random_pokemon", "POKEMON", "standard", 1, 2)'''
new_daily = '''                    new DailyRewardEntry("emi_roll", "EMI_ROLLS", "", 1, 5),
                    new DailyRewardEntry("treasure_ticket", "ITEM", "emipokemon:treasure_gacha_ticket", 1, 6),
                    new DailyRewardEntry("random_pokemon", "POKEMON", "standard", 1, 2)'''
require(old_daily in s, "daily reward defaults changed")
s = s.replace(old_daily, new_daily, 1)
write(p, s)

write("src/test/java/com/emipokemon/v050a1/Version050Alpha1RegressionTest.java", r'''package com.emipokemon.v050a1;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class Version050Alpha1RegressionTest {
    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }

    @Test void releaseVersionIs050Alpha1() throws Exception {
        assertTrue(read("gradle.properties").contains("mod_version=0.5.0-alpha.1"));
        assertTrue(read("src/main/java/com/emipokemon/Emipokemon.java").contains("VERSION = \"0.5.0-alpha.1\""));
    }

    @Test void treasureSwordClearsTheBillboard() throws Exception {
        String renderer = read("src/client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java");
        assertTrue(renderer.contains("matrices.translate(0.5D, 5.18D + bob, 0.5D);"));
        assertFalse(renderer.contains("matrices.translate(0.5D, 4.50D + bob, 0.5D);"));
    }

    @Test void passIncludesTreasureTicketsWithoutRemovingEmiTickets() throws Exception {
        String service = read("src/main/java/com/emipokemon/rewards/BattlePassService.java");
        assertTrue(service.contains("emipokemon:treasure_gacha_ticket"));
        assertTrue(service.contains("emipokemon:emi_special_banner_ticket"));
        assertTrue(service.contains("Ticket de Tesoros de Emi"));
        assertTrue(service.contains("Tickets de Tesoros de Emi"));
    }

    @Test void dailyRewardsIncludeAndMigrateTreasureTicket() throws Exception {
        String config = read("src/main/java/com/emipokemon/config/EmipokemonConfig.java");
        assertTrue(config.contains("configVersion = 10"));
        assertTrue(config.contains("migrateTreasureTicketRewards"));
        assertTrue(config.contains("\"treasure_ticket\", \"ITEM\", \"emipokemon:treasure_gacha_ticket\", 1, 6"));
    }

    @Test void approvedToolAndArmorRegressionsRemainPresent() {
        assertTrue(Files.exists(Path.of("src/test/java/com/emipokemon/alpha98/Alpha98VanillaHandheldToolsRegressionTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/emipokemon/alpha90/Alpha90HeartUvRegressionTest.java")));
    }
}
''')

write("CHANGELOG-0.5.0-alpha.1.md", '''# Emipokemon 0.5.0-alpha.1

## Inicio de la rama 0.5

- Base exacta: Emipokemon 0.4.0-alpha.98 validada.
- Tesoros de Emi: la espada flotante sube de Y 4.50 a Y 5.18 para dejar libre el texto de dos líneas.
- Pase: conserva los tickets especiales de Emi y añade Tickets de Tesoros de Emi como recompensa recurrente.
- Login diario: añade Ticket de Tesoros de Emi al pool de premios y migra configuraciones existentes sin borrar personalizaciones.
- Conserva las herramientas vanilla-handheld aprobadas de alpha.98 y la armadura GeckoLib aprobada de alpha.90.
''')

write("GUIA-PRUEBAS-0.5.0-alpha.1.md", '''# Guía de pruebas — Emipokemon 0.5.0-alpha.1

1. Tesoros de Emi: confirmar que la espada flotante ya no cruza “TESOROS DE EMI / Equipamiento exclusivo”.
2. Pase: revisar varios niveles y confirmar que aparecen Tickets de Tesoros de Emi y que los tickets especiales de Emi siguen existiendo.
3. Login diario: abrir /diario y confirmar que el Ticket de Tesoros de Emi aparece entre los premios posibles.
4. Reclamar un Ticket de Tesoros desde pase o diario y confirmar que se entrega `emipokemon:treasure_gacha_ticket`.
5. Confirmar que las cinco herramientas Emi conservan el aspecto aprobado de alpha.98.
6. Confirmar que la armadura Emi conserva el render aprobado de alpha.90.
''')

print("0.5.0-alpha.1 patch applied")
