from pathlib import Path

root = Path('.')

def read(rel):
    return (root / rel).read_text()

def write(rel, text):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text)

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'missing alpha37 anchor: {label}')
    return text.replace(old, new, 1)

# Version bump only; casino multiplayer implementation remains the alpha.36 implementation.
p = 'gradle.properties'
s = read(p)
s = replace_once(s, 'mod_version=0.4.0-alpha.36', 'mod_version=0.4.0-alpha.37', 'gradle version')
write(p, s)

p = 'src/main/java/com/emipokemon/Emipokemon.java'
s = read(p)
s = replace_once(s, '0.4.0-alpha.36', '0.4.0-alpha.37', 'core version')
write(p, s)

# CasinoScreen: never run Minecraft's blur pass after drawing casino content.
# Draw a deterministic dim overlay ourselves, and make renderBackground a no-op like the
# other Emipokemon screens that are known to remain sharp inside Cobbleverse.
p = 'src/client/java/com/emipokemon/client/casino/CasinoScreen.java'
s = read(p)
s = replace_once(s, '    private static final int PANEL = 0xF2140819;\n', '    private static final int PANEL = 0xFF160B1E;\n', 'opaque panel')
s = replace_once(s, '    private static final int PANEL_2 = 0xE91F1028;\n', '    private static final int PANEL_2 = 0xFF24142F;\n', 'opaque secondary panel')
s = replace_once(s, '    private static final int MUTED = 0xFFBDAFC5;\n', '    private static final int MUTED = 0xFFD8C9E2;\n', 'readable muted text')
s = replace_once(s, '    private static final int WHITE = 0xFFF8F2FF;\n', '    private static final int WHITE = 0xFFFFFFFF;\n', 'crisp white text')
s = replace_once(
    s,
    '        renderBackground(context, mouseX, mouseY, delta);\n',
    '        context.fill(0, 0, width, height, 0x99000000);\n',
    'manual dim background'
)
anchor = '    @Override public boolean shouldPause() { return false; }\n'
insert = '''    @Override\n    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {\n        // Intentionally empty. Screen.render() may call this after our widgets; allowing the\n        // vanilla implementation here applies Cobbleverse/Minecraft post-process blur over\n        // the already-rendered casino panel and makes every label appear out of focus.\n    }\n\n'''
if insert not in s:
    s = replace_once(s, anchor, insert + anchor, 'renderBackground override')
write(p, s)

write('CHANGELOG-0.4.0-alpha.37.md', '''# Emipokemon 0.4.0-alpha.37\n\nCasino visual correction candidate.\n\n- Keeps the alpha.36 server-authoritative multiplayer casino logic unchanged.\n- Removes the post-process blur pass from the casino screen so text and controls remain sharp.\n- Makes casino panels opaque and increases text contrast.\n- Replaces the alpha.36 near-black/magenta machine palettes with distinct, brighter professional palettes per machine.\n- Keeps every casino model within the alpha.36 one-block geometry bounds and preserves GeckoLib animation bones.\n- Does not modify EmiProtecciones or Arlight.\n\nVisual validation in the real Cobbleverse client is still required.\n''')
