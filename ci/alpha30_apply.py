from pathlib import Path

root = Path('.')

def read(rel):
    return (root / rel).read_text()

def write(rel, text):
    (root / rel).write_text(text)

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'missing alpha30 patch anchor: {label}')
    return text.replace(old, new, 1)

# Version
p='gradle.properties'; s=read(p); s=replace_once(s,'mod_version=0.4.0-alpha.29','mod_version=0.4.0-alpha.30','gradle version'); write(p,s)
p='src/main/java/com/emipokemon/Emipokemon.java'; s=read(p); s=replace_once(s,'0.4.0-alpha.29','0.4.0-alpha.30','core version'); write(p,s)

# Resolve known placeholder/emote syntax directly. This no longer requires a synchronized
# entity marker to succeed; markers remain useful only for {hologram_id} when available.
p='src/client/java/com/emipokemon/client/emote/HologramTextResolver.java'; s=read(p)
s=replace_once(
    s,
    '    private static final Pattern EMOTE = Pattern.compile(":([A-Za-z0-9_]{1,64}):");',
    '    private static final Pattern EMOTE = Pattern.compile(":([A-Za-z0-9_]{1,64}):");\n'
    '    private static final Pattern PLACEHOLDER = Pattern.compile("\\\\{(?:player|displayname|uuid|michicoins|ping|fps|online|max_players|server|dimension|biome|x|y|z|time|date|hologram_id|emipokemon_version)\\\\}");',
    'placeholder pattern'
)
old='''    public static Text resolve(DisplayEntity.TextDisplayEntity display, Text serverText) {\n        if (display == null || serverText == null) return serverText == null ? Text.empty() : serverText;\n        String id = hologramId(display, serverText);\n        if (id == null) return serverText;\n        return resolveText(serverText, id);\n    }'''
new='''    public static Text resolve(DisplayEntity.TextDisplayEntity display, Text serverText) {\n        if (display == null || serverText == null) return serverText == null ? Text.empty() : serverText;\n        String id = hologramId(display, serverText);\n        if (id == null && !shouldResolve(serverText)) return serverText;\n        return resolveText(serverText, id == null ? "" : id);\n    }'''
s=replace_once(s,old,new,'resolver gate')
old='''    public static boolean isEmipokemonDisplay(DisplayEntity.TextDisplayEntity display, Text serverText) {\n        return display != null && serverText != null && hologramId(display, serverText) != null;\n    }'''
new='''    public static boolean isEmipokemonDisplay(DisplayEntity.TextDisplayEntity display, Text serverText) {\n        return display != null && serverText != null\n                && (hologramId(display, serverText) != null || shouldResolve(serverText));\n    }\n\n    public static boolean shouldResolve(Text serverText) {\n        if (serverText == null) return false;\n        String raw = serverText.getString();\n        return PLACEHOLDER.matcher(raw).find() || EMOTE.matcher(raw).find();\n    }'''
s=replace_once(s,old,new,'syntax based display detection')
write(p,s)

# The emote picker toggle is a mouse control. Vanilla PressableWidget also activates focused
# buttons with Enter/Space; explicitly reject keyboard activation so sending chat cannot open it.
p='src/client/java/com/emipokemon/client/emote/EmotesButtonWidget.java'; s=read(p)
anchor='''        this.selected = selected;\n    }\n\n    @Override\n    protected void renderWidget'''
replacement='''        this.selected = selected;\n    }\n\n    @Override\n    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {\n        return false;\n    }\n\n    @Override\n    public void setFocused(boolean focused) {\n        super.setFocused(false);\n    }\n\n    @Override\n    protected void renderWidget'''
s=replace_once(s,anchor,replacement,'mouse-only emotes button')
write(p,s)

# Regression expectations.
p='src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java'; s=read(p)
s=s.replace('alpha29VersionIsConsistentInSource','alpha30VersionIsConsistentInSource')
s=s.replace('0.4.0-alpha.29','0.4.0-alpha.30')
s=replace_once(
    s,
    '        assertTrue(resolver.contains("List<EmoteEntry> entries = StreamotesCatalog.all()"));',
    '        assertTrue(resolver.contains("List<EmoteEntry> entries = StreamotesCatalog.all()"));\n'
    '        assertTrue(resolver.contains("PLACEHOLDER.matcher(raw).find() || EMOTE.matcher(raw).find()"));\n'
    '        assertTrue(resolver.contains("id == null && !shouldResolve(serverText)"));',
    'syntax resolver assertions'
)
needle='''        assertTrue(service.contains("loaded.discard();"));'''
s=replace_once(
    s,
    needle,
    needle + '\n        String emotesButton = source("client/java/com/emipokemon/client/emote/EmotesButtonWidget.java");\n'
    '        assertTrue(emotesButton.contains("public boolean keyPressed(int keyCode, int scanCode, int modifiers)"));\n'
    '        assertTrue(emotesButton.contains("return false;"));\n'
    '        assertTrue(emotesButton.contains("super.setFocused(false);"));',
    'keyboard button regression assertions'
)
write(p,s)

(root/'CHANGELOG-0.4.0-alpha.30.md').write_text('''# Emipokemon 0.4.0-alpha.30\n\n- Mantiene `minecraft:text_display` y la oclusión validada en alpha.28.\n- Placeholders/emotes ya no dependen de tags, customName ni marca sincronizada para activar el resolver.\n- El cliente procesa directamente sintaxis reconocida (`{player}`, `{online}`, `:Emote:`, etc.).\n- `{hologram_id}` usa la identidad cuando está disponible y queda vacío como fallback seguro.\n- El botón `✦ Emotes` deja de aceptar Enter/espacio/foco de teclado: solo se abre con ratón.\n- Streamotes sigue siendo opcional y el texto normal no depende de él.\n\nPendiente de validación visual real en Cobbleverse.\n''')
