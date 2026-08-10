from pathlib import Path

p = Path('src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java')
s = p.read_text()
marker = '''    @Test\n    void vanillaTextDisplayKeepsRendererWhileClientMixinAddsPlaceholdersOptionalEmotesAndDepthTesting() throws Exception {'''
start = s.index(marker)
end = s.index('\n    @Test', start + len(marker))
replacement = '''    @Test\n    void vanillaTextDisplayKeepsRendererWhileClientMixinAddsPlaceholdersOptionalEmotesAndDepthTesting() throws Exception {\n        String mixin = source("client/java/com/emipokemon/client/mixin/TextDisplayHologramMixin.java");\n        String clientMixins = source("client/resources/emipokemon.client.mixins.json");\n        String streamotes = source("client/java/com/emipokemon/client/emote/HologramStreamotesClientService.java");\n        String vanilla = source("main/java/com/emipokemon/hologram/VanillaTextHologram.java");\n        assertTrue(clientMixins.contains("TextDisplayHologramMixin"));\n        assertTrue(mixin.contains("HologramStreamotesClientService.resolve(data.text())"));\n        assertTrue(mixin.contains("data.flags() & ~0x02"));\n        assertTrue(streamotes.contains("EmoticonRegistry"));\n        assertTrue(streamotes.contains("makeEmoteStyle"));\n        assertTrue(vanilla.contains("nbt.putByte(\\\"see_through\\\", (byte) 0)"));\n    }\n'''
s = s[:start] + replacement + s[end:]
p.write_text(s)
