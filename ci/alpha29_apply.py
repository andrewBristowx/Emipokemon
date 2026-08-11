from pathlib import Path

root = Path('.')

def read(rel):
    return (root / rel).read_text()

def write(rel, text):
    (root / rel).write_text(text)

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'missing alpha29 patch anchor: {label}')
    return text.replace(old, new, 1)

# Version
p='gradle.properties'; s=read(p); s=replace_once(s,'mod_version=0.4.0-alpha.27','mod_version=0.4.0-alpha.29','gradle version'); write(p,s)
p='src/main/java/com/emipokemon/Emipokemon.java'; s=read(p); s=replace_once(s,'0.4.0-alpha.27','0.4.0-alpha.29','core version'); write(p,s)

# Server runtime: respawn any loaded alpha26-alpha28 display once so the new synchronized
# Text marker definitely reaches every client, while keeping the persisted registry authoritative.
p='src/main/java/com/emipokemon/hologram/HologramService.java'; s=read(p)
old='''        for (HologramRegistryStore.Entry entry : HologramRegistryStore.all()) {\n            if (findLoaded(server, entry.id()) != null) continue;\n            ServerWorld world = worldFor(server, entry.world());\n            if (world != null) VanillaTextHologram.spawn(world, entry);\n        }'''
new='''        for (HologramRegistryStore.Entry entry : HologramRegistryStore.all()) {\n            ServerWorld world = worldFor(server, entry.world());\n            if (world == null) continue;\n\n            DisplayEntity.TextDisplayEntity loaded = findLoaded(server, entry.id());\n            if (loaded != null) {\n                // alpha.29 replaces the loaded display once so the synchronized Text payload\n                // definitely contains the invisible marker used by the client resolver. This\n                // avoids relying on an in-place NBT reload being propagated to an already\n                // tracked entity from alpha.26-alpha.28.\n                loaded.discard();\n            }\n            VanillaTextHologram.spawn(world, entry);\n        }'''
s=replace_once(s,old,new,'restoreAll')
write(p,s)

# Vanilla text_display: embed an invisible identity marker in the Text payload itself.
p='src/main/java/com/emipokemon/hologram/VanillaTextHologram.java'; s=read(p)
s=replace_once(s,'import net.minecraft.text.Text;','import net.minecraft.text.MutableText;\nimport net.minecraft.text.Style;\nimport net.minecraft.text.Text;','text imports')
s=replace_once(s,'    static final String TAG_PREFIX = "emipokemon:hologram:";','    static final String TAG_PREFIX = "emipokemon:hologram:";\n    static final String TEXT_MARKER_PREFIX = "emipokemon:hologram-text:";','text marker const')
old='''        Text plainText = Text.literal(entry.text()).styled(style -> style.withColor(entry.color() & 0xFFFFFF));\n        nbt.putString("text", Text.Serialization.toJsonString(plainText, world.getRegistryManager()));'''
new='''        // Empty root: no visible characters, but its insertion style metadata travels inside\n        // the same TextDisplayEntity.Data payload that Minecraft synchronizes to the client.\n        MutableText markedText = Text.empty().setStyle(Style.EMPTY.withInsertion(TEXT_MARKER_PREFIX + entry.id()));\n        markedText.append(Text.literal(entry.text()).styled(style -> style.withColor(entry.color() & 0xFFFFFF)));\n        nbt.putString("text", Text.Serialization.toJsonString(markedText, world.getRegistryManager()));'''
s=replace_once(s,old,new,'marked Text payload')
write(p,s)

# Client resolver: identify from synchronized Text, refresh Streamotes correctly, and fall back
# to the registry's direct fromName lookup.
p='src/client/java/com/emipokemon/client/emote/HologramTextResolver.java'; s=read(p)
s=replace_once(s,'public final class HologramTextResolver {\n    private static final Pattern EMOTE', 'public final class HologramTextResolver {\n    private static final String CUSTOM_NAME_PREFIX = "Emipokemon hologram ";\n    private static final String TEXT_MARKER_PREFIX = "emipokemon:hologram-text:";\n    private static final Pattern EMOTE','resolver marker consts')
s=replace_once(s,'String id = hologramId(display);','String id = hologramId(display, serverText);','resolver id source')
s=replace_once(s,'EmoteEntry entry = EMOTES.get(matcher.group(1).toLowerCase(Locale.ROOT));','String requestedName = matcher.group(1);\n            EmoteEntry entry = EMOTES.get(requestedName.toLowerCase(Locale.ROOT));\n            if (entry == null) entry = StreamotesBridge.lookup(requestedName);','direct emote lookup')
start=s.index('    private static String hologramId(DisplayEntity.TextDisplayEntity display) {')
end=s.index('    private static void refreshEmotes()', start)
replacement='''    public static boolean isEmipokemonDisplay(DisplayEntity.TextDisplayEntity display, Text serverText) {\n        return display != null && serverText != null && hologramId(display, serverText) != null;\n    }\n\n    private static String hologramId(DisplayEntity.TextDisplayEntity display, Text serverText) {\n        String insertion = serverText.getStyle().getInsertion();\n        if (insertion != null && insertion.startsWith(TEXT_MARKER_PREFIX)\n                && insertion.length() > TEXT_MARKER_PREFIX.length()) {\n            return insertion.substring(TEXT_MARKER_PREFIX.length()).strip().toLowerCase(Locale.ROOT);\n        }\n        for (Text sibling : serverText.getSiblings()) {\n            String siblingInsertion = sibling.getStyle().getInsertion();\n            if (siblingInsertion != null && siblingInsertion.startsWith(TEXT_MARKER_PREFIX)\n                    && siblingInsertion.length() > TEXT_MARKER_PREFIX.length()) {\n                return siblingInsertion.substring(TEXT_MARKER_PREFIX.length()).strip().toLowerCase(Locale.ROOT);\n            }\n        }\n        Text customName = display.getCustomName();\n        if (customName != null) {\n            String value = customName.getString();\n            if (value.startsWith(CUSTOM_NAME_PREFIX) && value.length() > CUSTOM_NAME_PREFIX.length()) {\n                return value.substring(CUSTOM_NAME_PREFIX.length()).strip().toLowerCase(Locale.ROOT);\n            }\n        }\n        final String prefix = "emipokemon:hologram:";\n        for (String tag : display.getCommandTags()) {\n            if (tag.startsWith(prefix) && tag.length() > prefix.length()) return tag.substring(prefix.length());\n        }\n        return null;\n    }\n\n'''
s=s[:start]+replacement+s[end:]
old='''    private static void refreshEmotes() {\n        int revision = StreamotesCatalog.revision();\n        if (revision == emoteRevision) return;\n        EMOTES.clear();\n        for (EmoteEntry entry : StreamotesCatalog.all()) {\n            EMOTES.putIfAbsent(entry.name().toLowerCase(Locale.ROOT), entry);\n        }\n        emoteRevision = revision;\n    }'''
new='''    private static void refreshEmotes() {\n        List<EmoteEntry> entries = StreamotesCatalog.all();\n        int revision = StreamotesCatalog.revision();\n        if (revision == emoteRevision) return;\n        EMOTES.clear();\n        for (EmoteEntry entry : entries) {\n            EMOTES.putIfAbsent(entry.name().toLowerCase(Locale.ROOT), entry);\n        }\n        emoteRevision = revision;\n    }'''
s=replace_once(s,old,new,'catalog refresh')
write(p,s)

# Streamotes reflection bridge: exact public method used by Streamotes itself.
p='src/client/java/com/emipokemon/client/emote/StreamotesBridge.java'; s=read(p)
s=replace_once(s,'    private static Method isLoading;','    private static Method isLoading;\n    private static Method fromName;','fromName field')
s=replace_once(s,'            Method resolvedIsLoading = registry.getMethod("isLoading");','            Method resolvedIsLoading = registry.getMethod("isLoading");\n            Method resolvedFromName = registry.getMethod("fromName", String.class);','fromName resolve')
s=replace_once(s,'            isLoading = resolvedIsLoading;','            isLoading = resolvedIsLoading;\n            fromName = resolvedFromName;','fromName publish')
needle='    static boolean isLoading() {'
lookup='''    static EmoteEntry lookup(String name) {\n        Method method = fromName;\n        if (method == null || name == null || name.isBlank()) return null;\n        try {\n            Object emoticon = method.invoke(null, name);\n            if (emoticon == null) return null;\n            String resolvedName = StreamotesBridge.name(emoticon);\n            if (resolvedName == null || resolvedName.isBlank()) return null;\n            return new EmoteEntry(emoticon, resolvedName, source(emoticon), style(emoticon));\n        } catch (ReflectiveOperationException | RuntimeException exception) {\n            return null;\n        }\n    }\n\n'''
if needle not in s: raise SystemExit('missing alpha29 patch anchor: bridge lookup insertion')
s=s.replace(needle,lookup+needle,1)
write(p,s)

# Renderer mixin: resolve only marked Emipokemon displays and keep depth testing forced.
p='src/client/java/com/emipokemon/client/mixin/TextDisplayHologramMixin.java'; s=read(p)
old='''        DisplayEntity.TextDisplayEntity.Data data = cir.getReturnValue();\n        if (data == null) return;\n        Text resolved = HologramTextResolver.resolve(entity, data.text());\n        if (resolved == data.text() || resolved.equals(data.text())) return;\n        cir.setReturnValue(new DisplayEntity.TextDisplayEntity.Data(\n                resolved, data.lineWidth(), data.textOpacity(), data.backgroundColor(), data.flags()));'''
new='''        DisplayEntity.TextDisplayEntity.Data data = cir.getReturnValue();\n        if (data == null || !HologramTextResolver.isEmipokemonDisplay(entity, data.text())) return;\n        Text resolved = HologramTextResolver.resolve(entity, data.text());\n        byte depthTestedFlags = (byte) (data.flags() & ~0x02);\n        boolean textChanged = resolved != data.text() && !resolved.equals(data.text());\n        boolean flagsChanged = depthTestedFlags != data.flags();\n        if (!textChanged && !flagsChanged) return;\n        cir.setReturnValue(new DisplayEntity.TextDisplayEntity.Data(\n                resolved, data.lineWidth(), data.textOpacity(), data.backgroundColor(), depthTestedFlags));'''
s=replace_once(s,old,new,'mixin body')
write(p,s)

# Regression test expectations.
p='src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java'; s=read(p)
s=s.replace('alpha27VersionIsConsistentInSource','alpha29VersionIsConsistentInSource')
s=s.replace('0.4.0-alpha.27','0.4.0-alpha.29')
s=s.replace('vanillaTextDisplayKeepsRendererWhileClientMixinAddsPlaceholdersAndOptionalEmotes','vanillaTextDisplayKeepsRendererWhileClientMixinAddsPlaceholdersOptionalEmotesAndDepthTesting')
s=replace_once(s,'        assertTrue(mixin.contains("new DisplayEntity.TextDisplayEntity.Data"));','        assertTrue(mixin.contains("new DisplayEntity.TextDisplayEntity.Data"));\n        assertTrue(mixin.contains("data.flags() & ~0x02"));\n        assertTrue(mixin.contains("HologramTextResolver.isEmipokemonDisplay(entity, data.text())"));','test mixin assertions')
s=replace_once(s,'        assertTrue(resolver.contains("{hologram_id}"));','        assertTrue(resolver.contains("{hologram_id}"));\n        assertTrue(resolver.contains("TEXT_MARKER_PREFIX"));\n        assertTrue(resolver.contains("serverText.getStyle().getInsertion()"));\n        assertTrue(resolver.contains("StreamotesBridge.lookup(requestedName)"));\n        assertTrue(resolver.contains("List<EmoteEntry> entries = StreamotesCatalog.all()"));','test resolver assertions')
s=replace_once(s,'        assertTrue(core.contains("HologramService.restoreAll(server);"));','        assertTrue(core.contains("HologramService.restoreAll(server);"));\n        assertTrue(service.contains("loaded.discard();"));','test restore assertion')
write(p,s)

(root/'CHANGELOG-0.4.0-alpha.28.md').write_text('''# Emipokemon 0.4.0-alpha.28\n\n- Corrige oclusión de text_display (`see_through=false`).\n- Intento de identidad cliente por customName; validación real mostró que placeholders seguían literales.\n- No validada para placeholders/emotes.\n''')
(root/'CHANGELOG-0.4.0-alpha.29.md').write_text('''# Emipokemon 0.4.0-alpha.29\n\n- Mantiene minecraft:text_display y oclusión de alpha.28.\n- Marca invisible dentro del propio Text sincronizado para identificar hologramas en cliente.\n- Placeholders por espectador vuelven a ejecutarse sobre un display identificable.\n- Streamotes: refresco correcto del catálogo y respaldo EmoticonRegistry.fromName.\n- Texto normal sigue independiente de Streamotes.\n- Reemplaza una vez displays persistidos antiguos al arrancar para propagar la nueva marca.\n\nPendiente de validación visual real en Cobbleverse.\n''')
