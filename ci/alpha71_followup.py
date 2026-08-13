from pathlib import Path

path = Path("src/test/java/com/emipokemon/rewards/Alpha64DailyAndBattlePassRegressionTest.java")
text = path.read_text(encoding="utf-8")
old = '''        assertTrue(renderer.contains("getParameterCount() == 16"));
        assertTrue(renderer.contains("1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F"));'''
new = '''        assertTrue(renderer.contains("com.cobblemon.mod.common.api.gui.GuiUtilsKt"));
        assertTrue(renderer.contains("getParameterCount() != 5"));
        assertTrue(renderer.contains("method.getName().equals(\\"drawProfile\\")"));
        assertTrue(!renderer.contains("PokemonGuiUtilsKt"));'''
if old not in text:
    raise SystemExit("alpha64 portrait assertion block not found")
path.write_text(text.replace(old, new), encoding="utf-8")
print("alpha71 historical Cobblemon portrait regression updated")
