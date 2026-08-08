# Emipokemon — Hoja de ruta

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

1. **Core** — Base Fabric/Cobblemon/GeckoLib, configuración, datos, registro, comandos admin y validación de carga.
2. **Gacha backend** — Tiers, pools, pesos, probabilidades, pity, economía y entrega segura de Pokémon.
3. **Primer Gacha 3D** — Máquina, cápsulas, modelos, texturas, animaciones y sincronización servidor-cliente.
4. **Banners** — Banners permanentes/evento, rate-up, pity por banner y administración.
5. **Panel admin** — Configuración ingame de máquinas, pools, precios, banners y logs.
6. **Casino** — Slots, ruleta, dados y blackjack; RTP configurable y auditoría.
7. **Apuestas** — Moneda/Pokémon con bloqueo, confirmación, rollback y recuperación ante crash.
8. **Extras** — Recompensas diarias, eventos, rankings, tickets, temporadas y pulido.

## Fase 1 — Core

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

- Compila con Java 21.
- Minecraft 1.21.1 inicia con Emipokemon, Cobblemon y GeckoLib sin error de carga.
- La configuración se genera automáticamente.
- Los datos de prueba sobreviven reinicios.
- `/emipokemon version`, `/emipokemon status` y `/emipokemon reload` funcionan.
- Aún no hay lógica real de gacha/casino.
- Se valida en el pack real de Cobbleverse antes de etiquetar una versión estable.

## Reglas técnicas

- El servidor es autoritativo para premios y transacciones.
- Configuración antes que hardcode.
- No sobrescribir versiones estables sin validar la siguiente.
- GeckoLib se usa para presentación visual, no para decidir lógica de juego.
- No duplicar funciones cubiertas por mods administrativos externos.
