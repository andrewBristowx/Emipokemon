from pathlib import Path
import json

root = Path('.')

def read(rel):
    return (root / rel).read_text()

def write(rel, text):
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'missing alpha34 patch anchor: {label}')
    return text.replace(old, new, 1)

# Version.
p='gradle.properties'; s=read(p); s=replace_once(s,'mod_version=0.4.0-alpha.33','mod_version=0.4.0-alpha.34','gradle version'); write(p,s)
p='src/main/java/com/emipokemon/Emipokemon.java'; s=read(p); s=replace_once(s,'0.4.0-alpha.33','0.4.0-alpha.34','core version'); write(p,s)

# Alpha.32 resolves Streamotes at END_CLIENT_TICK. Keep that stable-state path, but expose
# the same resolver so the renderer can apply it synchronously on the very first frame after
# raw :emote: metadata arrives from the server.
p='src/client/java/com/emipokemon/client/emote/HologramStreamotesClientService.java'; s=read(p)
s=replace_once(s,'    static Text resolve(Text original) {','    public static Text resolve(Text original) {','public render resolver')
write(p,s)

# Re-enable the already-compatible TextDisplay renderer mixin, but use it only as a zero-frame
# Streamotes fallback. Placeholders stay server/per-viewer; see-through remains disabled.
p='src/client/java/com/emipokemon/client/mixin/TextDisplayHologramMixin.java'; s=read(p)
s=s.replace('import com.emipokemon.client.emote.HologramTextResolver;','import com.emipokemon.client.emote.HologramStreamotesClientService;')
old='''        DisplayEntity.TextDisplayEntity.Data data = cir.getReturnValue();\n        if (data == null || !HologramTextResolver.isEmipokemonDisplay(entity, data.text())) return;\n        Text resolved = HologramTextResolver.resolve(entity, data.text());\n        byte depthTestedFlags = (byte) (data.flags() & ~0x02);\n        boolean textChanged = resolved != data.text() && !resolved.equals(data.text());\n        boolean flagsChanged = depthTestedFlags != data.flags();\n        if (!textChanged && !flagsChanged) return;\n        cir.setReturnValue(new DisplayEntity.TextDisplayEntity.Data(\n                resolved, data.lineWidth(), data.textOpacity(), data.backgroundColor(), depthTestedFlags));'''
new='''        DisplayEntity.TextDisplayEntity.Data data = cir.getReturnValue();\n        if (data == null) return;\n\n        // Resolve Streamotes during getData(), before vanilla can draw a raw :emote: token.\n        // The END_CLIENT_TICK service remains the stable-state updater; this render-time path\n        // specifically removes the one-frame flash caused by periodic server metadata refreshes.\n        Text resolved = HologramStreamotesClientService.resolve(data.text());\n        byte depthTestedFlags = (byte) (data.flags() & ~0x02);\n        boolean textChanged = resolved != data.text() && !resolved.equals(data.text());\n        boolean flagsChanged = depthTestedFlags != data.flags();\n        if (!textChanged && !flagsChanged) return;\n        cir.setReturnValue(new DisplayEntity.TextDisplayEntity.Data(\n                resolved, data.lineWidth(), data.textOpacity(), data.backgroundColor(), depthTestedFlags));'''
s=replace_once(s,old,new,'renderer zero-frame resolver')
write(p,s)

p='src/client/resources/emipokemon.client.mixins.json'; obj=json.loads(read(p))
if 'TextDisplayHologramMixin' not in obj.get('client',[]):
    obj.setdefault('client',[]).append('TextDisplayHologramMixin')
write(p,json.dumps(obj,indent=2,ensure_ascii=False)+'\n')

# Update source version test and retire the historical alpha.31 assertion that required the
# renderer mixin to remain disabled. Add explicit anti-flicker coverage.
p='src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java'; s=read(p)
s=s.replace('alpha33VersionIsConsistentInSource','alpha34VersionIsConsistentInSource')
s=s.replace('0.4.0-alpha.33','0.4.0-alpha.34')
s=s.replace('        assertFalse(clientMixins.contains("TextDisplayHologramMixin"));','        assertTrue(clientMixins.contains("TextDisplayHologramMixin"));')
insert='''\n    @Test\n    void alpha34ResolvesEmotesBeforeFirstRenderedFrame() throws Exception {\n        String mixin = source("client/java/com/emipokemon/client/mixin/TextDisplayHologramMixin.java");\n        String clientMixins = source("client/resources/emipokemon.client.mixins.json");\n        String streamotes = source("client/java/com/emipokemon/client/emote/HologramStreamotesClientService.java");\n        assertTrue(clientMixins.contains("TextDisplayHologramMixin"));\n        assertTrue(mixin.contains("HologramStreamotesClientService.resolve(data.text())"));\n        assertTrue(mixin.contains("data.flags() & ~0x02"));\n        assertTrue(streamotes.contains("public static Text resolve(Text original)"));\n        assertTrue(streamotes.contains("END_CLIENT_TICK"));\n    }\n'''
pos=s.rfind('\n}')
if pos < 0: raise SystemExit('missing alpha34 patch anchor: test class closing brace')
s=s[:pos]+insert+s[pos:]
write(p,s)

write('CHANGELOG-0.4.0-alpha.34.md','''# Emipokemon 0.4.0-alpha.34\n\n- Mantiene placeholders por jugador, `minecraft:text_display`, oclusión y foco del chat de alpha.33.\n- Mantiene la resolución Streamotes cliente que ya mostró los emotes correctamente.\n- Elimina el microparpadeo de tokens `:emote:` añadiendo una resolución síncrona en `TextDisplayEntityRenderer.getData()` antes del dibujo.\n- Conserva el actualizador de fin de tick como estado estable y las resincronizaciones del servidor para no perder texto al retrackear entidades.\n- El fallback de renderer reutiliza exactamente el mismo registro/Style local de Streamotes.\n- No modifica EmiProtecciones ni Arlight.\n\nPendiente de validar visualmente en Cobbleverse que desaparece el parpadeo.\n''')
