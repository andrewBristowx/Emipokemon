from pathlib import Path

root = Path('.')

def read(rel):
    return (root / rel).read_text()

def write(rel, text):
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'missing alpha33 patch anchor: {label}')
    return text.replace(old, new, 1)

# Version only; preserve the alpha.32 placeholder/Streamotes architecture intact.
p = 'gradle.properties'
s = read(p)
s = replace_once(s, 'mod_version=0.4.0-alpha.32', 'mod_version=0.4.0-alpha.33', 'gradle version')
write(p, s)

p = 'src/main/java/com/emipokemon/Emipokemon.java'
s = read(p)
s = replace_once(s, '0.4.0-alpha.32', '0.4.0-alpha.33', 'core version')
write(p, s)

# Alpha.30 made the emotes button keyboard-inert, but also overrode setFocused() and
# forcibly cleared focus. In modded ChatScreen setups that can leave the chat field without
# focus until the player clicks it. Keep keyPressed() returning false, but stop interfering
# with Screen's normal focus ownership.
p = 'src/client/java/com/emipokemon/client/emote/EmotesButtonWidget.java'
s = read(p)
focus_override = '''    @Override\n    public void setFocused(boolean focused) {\n        super.setFocused(false);\n    }\n\n'''
s = replace_once(s, focus_override, '', 'remove forced button focus clearing')
write(p, s)

# Restore the vanilla chat field immediately after adding the overlay/button, then retain the
# already-existing next-tick restore as a compatibility fallback for other screen callbacks.
p = 'src/client/java/com/emipokemon/client/emote/ChatEmoteController.java'
s = read(p)
anchor = '''            // Adding the picker controls after ChatScreen initializes can leave\n            // another widget as the selected element in modded clients. Restore\n            // vanilla's chat-field focus on the following client tick, after all\n            // screen initialization callbacks have finished.\n            activeOverlay.focusChatOnNextTick();'''
replacement = '''            // Adding controls can change the selected element on heavily modded ChatScreens.\n            // Restore vanilla chat focus immediately, without making the Emotes button keyboard-active.\n            chatScreen.setFocused(field);\n            field.setFocused(true);\n\n            // Some mods run their own AFTER_INIT callbacks after ours, so restore it once more\n            // on the following client tick as a compatibility fallback.\n            activeOverlay.focusChatOnNextTick();'''
s = replace_once(s, anchor, replacement, 'restore chat focus immediately and next tick')
write(p, s)

# Regression coverage. Alpha.30 had an old expectation that the button must forcibly clear focus;
# alpha.33 intentionally reverses that expectation while keeping keyPressed() keyboard-inert.
p = 'src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java'
s = read(p)
s = s.replace('alpha32VersionIsConsistentInSource', 'alpha33VersionIsConsistentInSource')
s = s.replace('0.4.0-alpha.32', '0.4.0-alpha.33')
s = replace_once(
    s,
    '        assertTrue(emotesButton.contains("super.setFocused(false);"));',
    '        assertFalse(emotesButton.contains("super.setFocused(false);"));',
    'update alpha30 focus regression expectation'
)
insert = '''\n    @Test\n    void alpha33EmotesButtonDoesNotStealChatFocus() throws Exception {\n        String button = source("client/java/com/emipokemon/client/emote/EmotesButtonWidget.java");\n        String controller = source("client/java/com/emipokemon/client/emote/ChatEmoteController.java");\n        assertTrue(button.contains("public boolean keyPressed(int keyCode, int scanCode, int modifiers)"));\n        assertTrue(button.contains("return false;"));\n        assertFalse(button.contains("public void setFocused(boolean focused)"));\n        assertFalse(button.contains("super.setFocused(false)"));\n        assertTrue(controller.contains("chatScreen.setFocused(field);"));\n        assertTrue(controller.contains("field.setFocused(true);"));\n        assertTrue(controller.contains("activeOverlay.focusChatOnNextTick();"));\n    }\n'''
pos = s.rfind('\n}')
if pos < 0:
    raise SystemExit('missing alpha33 patch anchor: test class closing brace')
s = s[:pos] + insert + s[pos:]
write(p, s)

write('CHANGELOG-0.4.0-alpha.33.md', '''# Emipokemon 0.4.0-alpha.33\n\n- Conserva íntegros los placeholders por jugador, `minecraft:text_display`, oclusión y la integración Streamotes cliente de alpha.32.\n- Corrige la regresión de foco del chat introducida al hacer `✦ Emotes` solo-ratón.\n- El botón sigue rechazando activación por teclado (`Enter`/espacio), pero deja de forzar `setFocused(false)`.\n- Tras añadir los controles, Emipokemon devuelve inmediatamente el foco al campo de chat y vuelve a hacerlo en el siguiente tick como compatibilidad con otros mods.\n- No modifica EmiProtecciones ni Arlight.\n\nPendiente de validar en Cobbleverse tanto escritura inmediata en chat como `:fish:` de alpha.32.\n''')
