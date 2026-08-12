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
        raise SystemExit(f'missing alpha44 anchor: {label}')
    return text.replace(old, new, 1)

# Version bump. Gameplay and alpha.43 roulette logic stay unchanged.
p='gradle.properties'; s=read(p); s=replace_once(s,'mod_version=0.4.0-alpha.43','mod_version=0.4.0-alpha.44','gradle version'); write(p,s)
p='src/main/java/com/emipokemon/Emipokemon.java'; s=read(p); s=replace_once(s,'0.4.0-alpha.43','0.4.0-alpha.44','core version'); write(p,s)

# Restore the alpha.37 no-blur contract. The alpha.43 redesign correctly stopped calling
# renderBackground() directly, but dropped this override. In the Cobbleverse client the screen
# lifecycle can still invoke Screen.renderBackground(), applying the post-process blur over
# already-rendered widgets. Keep our manual dim overlay in render() and make the inherited
# background hook a no-op.
p='src/client/java/com/emipokemon/client/casino/CasinoScreen.java'
s=read(p)
anchor='    @Override public boolean shouldPause() { return false; }\n'
insert='''    @Override\n    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {\n        // Deliberately empty: the casino draws its own dim backdrop in render().\n        // Allowing Screen.renderBackground() here re-applies Cobbleverse/Minecraft blur\n        // over the casino UI and makes text, roulette numbers and widgets out of focus.\n    }\n\n'''
if insert not in s:
    s=replace_once(s,anchor,insert+anchor,'renderBackground no-op override')
write(p,s)

write('CHANGELOG-0.4.0-alpha.44.md','''# Emipokemon 0.4.0-alpha.44\n\nCasino GUI sharpness hotfix based on alpha.43.\n\n- Restores the proven alpha.37 no-op `renderBackground` override.\n- Keeps alpha.43 interactive roulette, clickable betting cloth, shared result animation and server-authoritative multiplayer unchanged.\n- Keeps the manual dim backdrop, so the world remains visually separated without post-process blur over the GUI.\n- Does not modify casino economy, anti-dupe, models, holograms, EmiProtecciones or Arlight.\n\nReal Cobbleverse visual validation is still required.\n''')
