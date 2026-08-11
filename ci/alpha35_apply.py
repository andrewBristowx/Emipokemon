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
        raise SystemExit(f'missing alpha35 patch anchor: {label}')
    return text.replace(old, new, 1)

p='gradle.properties'; s=read(p); s=replace_once(s,'mod_version=0.4.0-alpha.34','mod_version=0.4.0-alpha.35','gradle version'); write(p,s)
p='src/main/java/com/emipokemon/Emipokemon.java'; s=read(p); s=replace_once(s,'0.4.0-alpha.34','0.4.0-alpha.35','core version'); write(p,s)

p='src/main/java/com/emipokemon/hologram/HologramViewerTextService.java'; s=read(p)
s=replace_once(s,
    'import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;',
    'import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;\nimport net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;',
    'tracking event import')
old='''        ServerTickEvents.END_SERVER_TICK.register(HologramViewerTextService::tick);\n        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {'''
new='''        ServerTickEvents.END_SERVER_TICK.register(HologramViewerTextService::tick);\n        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {\n            if (trackedEntity instanceof DisplayEntity.TextDisplayEntity display) {\n                LAST_SENT.remove(new CacheKey(player.getUuid(), display.getId()));\n            }\n        });\n        EntityTrackingEvents.STOP_TRACKING.register((trackedEntity, player) -> {\n            if (trackedEntity instanceof DisplayEntity.TextDisplayEntity display) {\n                LAST_SENT.remove(new CacheKey(player.getUuid(), display.getId()));\n            }\n        });\n        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {'''
s=replace_once(s,old,new,'tracking cache invalidation')
old='''                if (previous != null && previous.fingerprint().equals(fingerprint)\n                        && ticks - previous.tick() < 100L) {\n                    continue;\n                }'''
new='''                if (previous != null && previous.fingerprint().equals(fingerprint)) {\n                    continue;\n                }'''
s=replace_once(s,old,new,'remove periodic resend')
write(p,s)

p='src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java'; s=read(p)
s=s.replace('alpha34VersionIsConsistentInSource','alpha35VersionIsConsistentInSource')
s=s.replace('0.4.0-alpha.34','0.4.0-alpha.35')
insert='''\n    @Test\n    void alpha35DoesNotPeriodicallyResendUnchangedHologramMetadata() throws Exception {\n        String viewer = source("main/java/com/emipokemon/hologram/HologramViewerTextService.java");\n        assertTrue(viewer.contains("EntityTrackingEvents.START_TRACKING"));\n        assertTrue(viewer.contains("EntityTrackingEvents.STOP_TRACKING"));\n        assertTrue(viewer.contains("previous.fingerprint().equals(fingerprint)"));\n        assertFalse(viewer.contains("ticks - previous.tick() < 100L"));\n    }\n'''
pos=s.rfind('\n}')
if pos < 0: raise SystemExit('missing alpha35 patch anchor: test class closing brace')
s=s[:pos]+insert+s[pos:]
write(p,s)

write('CHANGELOG-0.4.0-alpha.35.md','''# Emipokemon 0.4.0-alpha.35\n\n- Mantiene `minecraft:text_display`, placeholders por jugador, Streamotes, oclusión y foco de chat.\n- Elimina el reenvío periódico de metadata idéntica cada 100 ticks.\n- La caché por viewer solo se invalida por cambios reales o por START/STOP_TRACKING.\n- No modifica EmiProtecciones ni Arlight.\n\nPendiente de validación visual real en Cobbleverse.\n''')
