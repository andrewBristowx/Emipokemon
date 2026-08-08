# Emipokemon — Hoja de ruta

## Estado actual

- **Fase 1 — Core: VALIDADA ✅**
- **Fase 2 — Gacha backend: NÚCLEO VALIDADO ✅ / ECONOMÍA ITEM PENDIENTE 🧪**
- **Build actual de prueba:** `0.2.0-alpha.2`.
- Catálogo, clasificación, filtros de banner, entrega, envío a PC y persistencia de pity ya comprobados dentro de Cobbleverse.
- El falso negativo de entrega detectado en `alpha.1` quedó corregido y revalidado en `alpha.2`.

## Arquitectura prevista

- **Core:** configuración, registro, comandos, permisos y servicios comunes.
- **Data:** datos persistentes por jugador y recuperación segura.
- **Economy:** adaptadores para CobbleDollars, ítems y tickets propios.
- **Gacha:** catálogo de rarezas, tiers, pools, pesos, pity, banners y entrega segura de Pokémon.
- **Hub:** plantilla modular, Centro Pokémon, Poké Mart, zonas para Gacha, Casino, Eventos y servicios.
- **Casino:** slots, ruleta, dados, blackjack y auditoría.
- **Pokemon Betting:** apuestas PvP y transacciones seguras.
- **Rewards:** recompensas diarias, eventos y premios.
- **Visual:** modelos, bloques, objetos, animaciones, partículas y sonidos con GeckoLib.

## Fases

1. **Core — VALIDADA ✅** — Base Fabric/Cobblemon/GeckoLib, configuración, datos, registro, comandos admin y validación de carga.
2. **Gacha backend — NÚCLEO VALIDADO ✅ / ECONOMÍA ITEM PENDIENTE 🧪** — Catálogo automático de rarezas, filtros, tiers, pools, pesos, probabilidades, pity, economía básica y entrega segura de Pokémon.
3. **Primer Gacha 3D** — Máquina, cápsulas, modelos, texturas, animaciones y sincronización servidor-cliente.
4. **Hub + Centro Pokémon + Poké Mart** — Sistema de plantilla modular, plaza central, spawn, Centro Pokémon funcional, tienda configurable y espacios reservados para Gacha/Casino/Eventos.
5. **Banners** — Banners permanentes/evento, rate-up avanzado, pity por banner y rotación de contenido.
6. **Panel admin** — Configuración ingame de máquinas, pools, precios, banners, Hub y logs.
7. **Casino** — Slots, ruleta, dados y blackjack; RTP configurable y auditoría.
8. **Apuestas** — Moneda/Pokémon con bloqueo, confirmación, rollback y recuperación ante crash.
9. **Extras** — Recompensas diarias, eventos, rankings, tickets, temporadas y pulido.

## Fase 1 — Core

### Estado

**VALIDADA — 2026-08-08**

Pruebas completadas en el pack real:

- Carga correcta de Emipokemon junto con Cobbleverse/Cobblemon/GeckoLib.
- `/emipokemon version`, `/emipokemon status`, `/emipokemon reload` y `/emipokemon debug` funcionando.
- Configuración `v1` cargada correctamente.
- Registro de jugador cargado en memoria (`player data loaded: 1`).
- Persistencia tras desconectar/reconectar confirmada (`debugCounter` 1 → 2).
- Persistencia tras apagado y reinicio completo confirmada (`debugCounter` 2 → 3).
- Build CI exitoso con Java 21, Fabric Loom 1.17.13, Cobblemon 1.7.3 y GeckoLib 4.9.2.

## Fase 2 — Gacha backend

### Estado

**NÚCLEO VALIDADO EN SERVIDOR REAL — pendiente únicamente la prueba de economía `ITEM` antes de cerrar la fase al 100%.**

Build actual: **Emipokemon 0.2.0-alpha.2**.

### Pruebas reales ya completadas

- El catálogo detectó **1025 Pokémon** cargados por Cobbleverse/Cobblemon y addons.
- Distribución observada: `COMMON=313`, `UNCOMMON=196`, `RARE=347`, `EPIC=64`, `LEGENDARY=82`, `MYTHICAL=23`.
- Rayquaza detectado como `LEGENDARY`, Gen 3, Hoenn, Dragón/Volador.
- Deoxys detectado como `MYTHICAL`, Gen 3, Hoenn.
- Charmander detectado como `EPIC` por label `starter`.
- Metagross detectado como `EPIC` por label `powerhouse`.
- `rayquaza_hoenn` construyó automáticamente un pool de Gen 3 con `COMMON=51`, `UNCOMMON=28`, `RARE=38`, `EPIC=8`, `LEGENDARY=8`, `MYTHICAL=2`.
- Las tiradas reales entregan Pokémon correctamente.
- Con el equipo lleno, Cobblemon envía correctamente los premios al PC.
- En `0.2.0-alpha.1` se detectó que Cobblemon podía entregar correctamente pero devolver valor numérico `0`; Emipokemon lo interpretaba como fallo, devolvía la moneda y no registraba pity.
- `0.2.0-alpha.2` corrigió la validación usando el indicador booleano de éxito del comando.
- La corrección fue revalidada: una tirada real confirma correctamente el premio y aumenta el pity.
- El pity persistió después de reiniciar completamente el servidor (`total=2`, `épico=2/10`, `legendario=2/90`).

### Catálogo de Pokémon y rarezas

- El catálogo se construye automáticamente desde las especies implementadas cargadas por Cobblemon.
- Lee número de Pokédex, generación, región, tipos, labels, catch rate y estadísticas base.
- Clasificación propia: `COMMON`, `UNCOMMON`, `RARE`, `EPIC`, `LEGENDARY`, `MYTHICAL`, `SPECIAL`.
- Labels oficiales como `legendary`, `mythical`, `starter`, `powerhouse` y `paradox` influyen directamente en el tier.
- Los Pokémon dentro de un mismo tier reciben un peso natural basado también en catch rate, por lo que no todos tienen exactamente la misma facilidad.
- `config/emipokemon/gacha/catalog_overrides.json` permite corregir manualmente el tier de cualquier especie, incluida una especie añadida por addons.

### Banners y filtros

Los banners son JSON configurables y pueden filtrar por generación, región, tipo, labels requeridos/excluidos, especies excluidas y tiers permitidos.

También pueden configurar pesos de cada tier, Pokémon destacados, niveles, shiny chance, moneda/precio y pity.

Banner de prueba actual:

- **`rayquaza_hoenn` — Rayquaza: Cielos de Hoenn**
- filtro de Generación 3;
- Rayquaza destacado con multiplicador `x6`;
- resto del pool generado automáticamente.

### Pity y datos persistentes

- Pity separado por jugador y por banner.
- Garantía de Épico o superior configurable (por defecto 10 tiradas).
- Soft pity legendario configurable (por defecto desde la tirada 60).
- Hard pity Legendario o superior configurable (por defecto 90 tiradas).
- El progreso se guarda inmediatamente después de una entrega confirmada.
- Persistencia de pity tras reinicio completo del servidor: **VALIDADA ✅**.

### Economía y entrega

- Backend `FREE` para pruebas.
- Backend `ITEM` para cobrar cualquier ítem configurable por ID.
- Si la entrega falla realmente después de retirar un ítem, el coste se devuelve.
- La entrega usa los comandos de Cobblemon y valida parseo + resultado de ejecución.
- Integración directa con CobbleDollars queda para cuando fijemos la economía definitiva.

### Comandos de prueba

- `/emipokemon gacha catalog`
- `/emipokemon gacha inspect <pokemon>`
- `/emipokemon gacha banners`
- `/emipokemon gacha info <banner>`
- `/emipokemon gacha pity <banner>`
- `/emipokemon gacha simulate <banner>` — admin; no entrega ni consume.
- `/emipokemon gacha pull <banner>` — admin durante la alpha.
- `/emipokemon gacha reload` — admin.

### Criterios para validar la Fase 2

- [x] Compila en CI con la baseline real.
- [x] El catálogo detecta correctamente especies de Cobbleverse y addons instalados.
- [x] Rayquaza aparece como `LEGENDARY`, Deoxys como `MYTHICAL` y starters/powerhouse en su tier esperado.
- [x] `rayquaza_hoenn` genera un pool compatible con Gen 3.
- [x] `pull` entrega Pokémon reales.
- [x] Con equipo lleno, Cobblemon manda el premio al PC.
- [x] En `alpha.2` una entrega correcta confirma la tirada y aumenta pity.
- [x] Persistencia del pity tras reinicio completo.
- [ ] Confirmar explícitamente que `simulate` no altera pity ni entrega Pokémon.
- [ ] Probar una transacción `ITEM` y su reembolso solo ante fallo real.

## Fase 4 — Hub + Centro Pokémon + Poké Mart

Esta fase se realizará después de tener el primer Gacha 3D funcional para diseñar el spawn alrededor de sistemas reales.

### 4A — Sistema de Hub

- `/emipokemon hub create` para colocar una plantilla controlada por Emipokemon.
- Spawn principal, `/hub`, teletransporte y puntos de servicio.
- Plantillas modulares para actualizar una zona sin reemplazar todo el Hub.
- Backups/revisiones antes de reemplazar estructuras.
- Protección para jugadores y modo de edición para administradores.

### 4B — Centro Pokémon

- Edificio principal integrado en la plantilla.
- Curación del equipo.
- Acceso a PC.
- Espacio para intercambio y futuros servicios.
- Terminales/NPCs visuales donde aporte valor.

### 4C — Poké Mart / Tienda Pokémon

- Tienda de Poké Balls, medicinas y suministros.
- Categorías y precios totalmente configurables.
- Preparada para usar la economía definitiva del servidor.
- Posibilidad futura de tienda rotativa o artículos especiales.

### 4D — Plaza y zonas reservadas

- Gacha funcional.
- Parcela de Casino preparada sin implementar aún su lógica.
- Zona de Eventos, Kits/Recompensas, Rankings e Información.
- Actualizaciones modulares (`hub_core`, `pokecenter`, `pokemart`, `gacha`, `casino`, etc.).

## Reglas técnicas

- El servidor es autoritativo para premios y transacciones.
- Configuración antes que hardcode.
- No sobrescribir versiones estables sin validar la siguiente.
- GeckoLib se usa para presentación visual, no para decidir lógica de juego.
- No duplicar funciones cubiertas por mods administrativos externos.
- Los filtros de banners reutilizan un catálogo central; no se duplican listas completas de Pokémon por banner.
- Las plantillas del Hub serán modulares para evitar regenerar zonas ya decoradas o configuradas.
