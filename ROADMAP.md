# Emipokemon — Hoja de ruta

## Estado actual

- **Fase 1 — Core: VALIDADA** ✅
- **Siguiente:** Fase 2 — Gacha backend
- Validación real realizada en Cobbleverse 1.21.1 con Cobblemon 1.7.3.
- Persistencia confirmada tras reconexión y tras reinicio completo del servidor (`debugCounter` 1 → 2 → 3).

## Arquitectura prevista

- **Core:** configuración, registro, comandos, permisos y servicios comunes.
- **Data:** datos persistentes por jugador y recuperación segura.
- **Economy:** adaptadores para CobbleDollars, ítems y tickets propios.
- **Gacha:** tiers, pesos, pity, banners y entrega segura de Pokémon.
- **Casino:** slots, ruleta, dados, blackjack y auditoría.
- **Pokemon Betting:** apuestas PvP y transacciones seguras.
- **Rewards:** recompensas diarias, eventos y premios.
- **Visual:** modelos, bloques, objetos, animaciones, partículas y sonidos con GeckoLib.

## Fases

1. **Core — VALIDADA ✅** — Base Fabric/Cobblemon/GeckoLib, configuración, datos, registro, comandos admin y validación de carga.
2. **Gacha backend** — Tiers, pools, pesos, probabilidades, pity, economía y entrega segura de Pokémon.
3. **Primer Gacha 3D** — Máquina, cápsulas, modelos, texturas, animaciones y sincronización servidor-cliente.
4. **Banners** — Banners permanentes/evento, rate-up, pity por banner y administración.
5. **Panel admin** — Configuración ingame de máquinas, pools, precios, banners y logs.
6. **Casino** — Slots, ruleta, dados y blackjack; RTP configurable y auditoría.
7. **Apuestas** — Moneda/Pokémon con bloqueo, confirmación, rollback y recuperación ante crash.
8. **Extras** — Recompensas diarias, eventos, rankings, tickets, temporadas y pulido.

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

### Alcance

- Proyecto Fabric 1.21.1 con Java 21.
- Compatibilidad objetivo: Cobblemon 1.7.3, Fabric API 0.116.14 y GeckoLib 4.9.2.
- Mod id `emipokemon`.
- Estructura de paquetes preparada para módulos futuros.
- Configuración JSON versionada y recargable.
- Almacenamiento persistente por UUID de jugador.
- Comandos administrativos base: `version`, `status`, `reload` y `debug`.
- Puntos centrales de registro para futuros bloques, ítems, sonidos y networking.
- Separación estricta entre lógica común/servidor y renderizado cliente.
- Logging y manejo seguro de configuraciones dañadas.

### Criterios de aceptación

- [x] Compila con Java 21.
- [x] Minecraft 1.21.1 inicia con Emipokemon, Cobblemon y GeckoLib sin error de carga.
- [x] La configuración se genera automáticamente.
- [x] Los datos de prueba sobreviven reinicios.
- [x] `/emipokemon version`, `/emipokemon status` y `/emipokemon reload` funcionan.
- [x] Aún no hay lógica real de gacha/casino.
- [x] Validado en el pack real de Cobbleverse antes de considerar cerrada la fase.

## Reglas técnicas

- El servidor es autoritativo para premios y transacciones.
- Configuración antes que hardcode.
- No sobrescribir versiones estables sin validar la siguiente.
- GeckoLib se usa para presentación visual, no para decidir lógica de juego.
- No duplicar funciones cubiertas por mods administrativos externos.
