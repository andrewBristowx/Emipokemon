# Emipokemon 0.4.0-alpha.27 — GitHub build request

Fuente autoritativa usada por CI: `Emipokemon-0.4.0-alpha.27-source.zip`.
SHA-256: `A7E5545668760F84D036EE017622D32D9B8FA0FFE4B77CFDAE10D40C820ABF4B`.

Objetivo de esta candidata:
- mantener `minecraft:text_display` validado en alpha.26;
- añadir placeholders locales por cliente;
- añadir Streamotes de forma opcional;
- mantener texto normal independiente de Streamotes;
- desactivar `see_through` para respetar la profundidad de bloques.

El workflow debe compilar con Temurin Java 21 mediante `./gradlew clean test build --no-daemon --stacktrace`. La compilación no equivale a validación visual en Cobbleverse.
