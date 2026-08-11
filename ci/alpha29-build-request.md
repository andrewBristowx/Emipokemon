# Emipokemon 0.4.0-alpha.29 — GitHub build request

Objetivo de esta candidata:
- mantener `minecraft:text_display` y la oclusión validada en alpha.28;
- identificar el holograma desde una marca invisible dentro del `Text` sincronizado;
- resolver placeholders por cliente;
- refrescar correctamente el catálogo de Streamotes;
- usar `EmoticonRegistry.fromName` como respaldo para emotes;
- mantener el texto normal independiente de Streamotes.

Compilar exclusivamente con Temurin Java 21 y `./gradlew clean test build --no-daemon --stacktrace`.

No considerar placeholders/emotes validados hasta la prueba real en Cobbleverse.
