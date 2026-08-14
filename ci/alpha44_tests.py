from pathlib import Path

root = Path('.')

# Advance inherited version assertions only.
for p in (root/'src/test/java').rglob('*.java'):
    s = p.read_text()
    s = s.replace('0.4.0-alpha.43','0.4.0-alpha.44')
    s = s.replace('alpha43VersionIsConsistentInSource','alpha44VersionIsConsistentInSource')
    s = s.replace('alpha43VersionIsConsistent','alpha44VersionIsConsistent')
    p.write_text(s)

# Replace the alpha.43 no-blur regression with the stronger proven contract: no direct call
# AND an explicit no-op override preventing the screen lifecycle from applying blur later.
p = root/'src/test/java/com/emipokemon/casino/CasinoRouletteGuiRegressionTest.java'
s = p.read_text()
old = '''    @Test\n    void casinoScreenKeepsTheNoBlurRenderingContract() throws Exception {\n        String s=screen();\n        assertFalse(s.contains("renderBackground(context"),"casino GUI must not blur itself in Cobbleverse");\n    }\n'''
new = '''    @Test\n    void casinoScreenKeepsTheNoBlurRenderingContract() throws Exception {\n        String s=screen();\n        assertFalse(s.contains("        renderBackground(context, mouseX, mouseY, delta);"),\n                "casino render must not invoke vanilla blur directly");\n        assertTrue(s.contains("public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)"),\n                "casino screen must override the inherited background hook");\n        assertTrue(s.contains("Deliberately empty: the casino draws its own dim backdrop"),\n                "the inherited background hook must remain an intentional no-op");\n        assertTrue(s.contains("context.fill(0, 0, width, height, BACKDROP)"),\n                "casino must draw a deterministic dim backdrop itself");\n    }\n'''
if old not in s:
    raise SystemExit('missing alpha43 no-blur regression anchor')
s = s.replace(old,new,1)
p.write_text(s)

# Keep the older visual regression aligned with the behavior rather than alpha.37 literal colors.
p = root/'src/test/java/com/emipokemon/casino/CasinoVisualRegressionTest.java'
s = p.read_text()
start = s.index('    @Test\n    void alpha37CasinoScreenDoesNotBlurItsOwnUi()')
end = s.index('\n    @Test', start + 10)
replacement = '''    @Test\n    void alpha37CasinoScreenDoesNotBlurItsOwnUi() throws Exception {\n        String screen = source("client/java/com/emipokemon/client/casino/CasinoScreen.java");\n        assertTrue(screen.contains("public void renderBackground(DrawContext context"));\n        assertFalse(screen.contains("        renderBackground(context, mouseX, mouseY, delta);"));\n        assertTrue(screen.contains("context.fill(0, 0, width, height, BACKDROP)"));\n    }\n'''
s = s[:start] + replacement + s[end:]
p.write_text(s)
