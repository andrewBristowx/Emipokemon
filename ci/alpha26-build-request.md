# Emipokemon 0.4.0-alpha.26 — GitHub build request

Esta rama existe para disparar la compilación reproducible de alpha.26 en GitHub Actions.

Fuente CI actual: `Emipokemon-0.4.0-alpha.26-source-ci2.zip`.
SHA-256: `977F1FF882B55DA2DA9D41172900524C88A7158652204AC2A2CFB6E6DA293BFC`.

Corrección CI2: `HologramService.findLoaded` conserva el tipo wildcard devuelto por `getEntitiesByType` mediante `var`, evitando la asignación inválida a `List<TextDisplayEntity>`.

El workflow compila con Temurin Java 21 mediante `./gradlew clean test build --no-daemon --stacktrace` y no implica validación visual de Cobbleverse.
