# Emipokemon — Hoja de ruta

## Estado actual

- **Fase 1 — Core: VALIDADA ✅**
- **Fase 2 — Gacha backend: IMPLEMENTADA Y COMPILA ✅ / PENDIENTE VALIDACIÓN REAL 🧪**
- **Siguiente validación:** probar Fase 2 dentro del pack real de Cobbleverse.
- Validación real de Fase 1 realizada en Cobbleverse 1.21.1 con Cobblemon 1.7.3.
- Persistencia confirmada tras reconexión y tras reinicio completo del servidor (`debugCounter` 1 → 2 → 3).

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
2. **Gacha backend — IMPLEMENTADA / COMPILA ✅ / PENDIENTE PRUEBA REAL 🧪** — Catálogo automático de rarezas, filtros, tiers, pools, pesos, probabilidades, pity, economía básica y entrega segura de Pokémon.
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

**IMPLEMENTADA Y COMPILA EN CI — pendiente validación dentro del servidor real**

Build de desarrollo: **Emipokemon 0.2.0-alpha.1**.

### Catálogo de Pokémon y rarezas

- El catálogo se construye automáticamente desde las especies implementadas cargadas por Cobblemon.
- Lee número de Pokédex, generación, región, tipos, labels, catch rate y estadísticas base.
- Clasificación propia: `COMMON`, `UNCOMMON`, `RARE`, `EPIC`, `LEGENDARY`, `MYTHICAL`, `SPECIAL`.
- Labels oficiales como `legendary`, `mythical`, `starter`, `powerhouse` y `paradox` influyen directamente en el tier.
- Los Pokémon dentro de un mismo tier reciben un peso natural basado también en catch rate, por lo que no todos tienen exactamente la misma facilidad.
- `config/emipokemon/gacha/catalog_overrides.json` permite corregir manualmente el tier de cualquier especie, incluida una especie añadida por addons.

### Banners y filtros

Los banners son JSON configurables y pueden filtrar por:

- generación;
- región;
- tipo;
- labels requeridos;
- labels excluidos;
- especies excluidas;
- tiers permitidos.

También pueden configurar:

- pesos de cada tier;
- Pokémon destacados y multiplicador de rate-up;
- nivel mínimo/máximo por tier;
- probabilidad shiny por tier;
- moneda y precio;
- soft pity, hard pity y garantía épica.

Se crea automáticamente un banner de ejemplo:

- **`rayquaza_hoenn` — Rayquaza: Cielos de Hoenn**
- filtro de Generación 3;
- Rayquaza destacado con multiplicador de peso `x6`;
- el resto del pool se construye automáticamente con Pokémon válidos de esa generación.

### Pity y datos persistentes

- Pity separado por jugador y por banner.
- Garantía de Épico o superior configurable (por defecto 10 tiradas).
- Soft pity legendario configurable (por defecto desde la tirada 60).
- Hard pity Legendario o superior configurable (por defecto 90 tiradas).
- El progreso se guarda inmediatamente después de una entrega exitosa y sobrevive reinicios.

### Economía y entrega

- Backend de moneda `FREE` para pruebas.
- Backend `ITEM` para cobrar cualquier ítem configurable por ID.
- Si la entrega del Pokémon falla después de retirar un ítem, el coste se devuelve al jugador.
- La entrega usa la ruta de comandos de Cobblemon y captura el resultado real de ejecución antes de confirmar la transacción.
- La integración directa con CobbleDollars queda preparada como adaptador futuro cuando fijemos la economía definitiva del servidor.

### Comandos de prueba de Fase 2

- `/emipokemon gacha catalog`
- `/emipokemon gacha inspect <pokemon>` — muestra tier, generación, región, tipos, catch rate, BST y labels del catálogo.
- `/emipokemon gacha banners`
- `/emipokemon gacha info <banner>`
- `/emipokemon gacha pity <banner>`
- `/emipokemon gacha simulate <banner>` — admin, no entrega ni consume.
- `/emipokemon gacha pull <banner>` — admin durante la alpha; tirada real temporal para validar el backend.
- `/emipokemon gacha reload` — admin; recarga banners y catálogo/overrides.

### Criterios para validar la Fase 2

- [x] Compila en CI con la baseline real.
- [ ] El catálogo detecta correctamente las especies de Cobbleverse y addons instalados.
- [ ] Rayquaza aparece como `LEGENDARY`, Deoxys como `MYTHICAL` y los starters/powerhouse en su tier esperado.
- [ ] `rayquaza_hoenn` solo genera un pool compatible con sus filtros de generación.
- [ ] `simulate` funciona sin alterar pity ni entregar Pokémon.
- [ ] `pull` entrega un Pokémon real correctamente.
- [ ] Con equipo lleno, Cobblemon gestiona correctamente el destino del premio.
- [ ] El pity aumenta, se reinicia al obtener el tier correspondiente y persiste tras reinicio.
- [ ] Un coste `ITEM` se retira y se reembolsa si la entrega falla.

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
