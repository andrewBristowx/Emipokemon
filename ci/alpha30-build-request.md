# Emipokemon 0.4.0-alpha.30 — GitHub build request

Objetivo:
- mantener la oclusión validada de `minecraft:text_display`;
- resolver placeholders/emotes por sintaxis reconocida sin depender de tags/customName/marcadores;
- mantener Streamotes opcional;
- impedir que el botón `✦ Emotes` se active con Enter/espacio o foco de teclado.

Compilar exclusivamente con Temurin Java 21 y `./gradlew clean test build --no-daemon --stacktrace`.

No considerar placeholders/emotes validados hasta prueba real en Cobbleverse.
