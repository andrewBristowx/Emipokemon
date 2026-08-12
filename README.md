# Emipokemon

Mod central personalizado para el servidor Cobbleverse.

## Baseline

- Minecraft 1.21.1
- Fabric
- Cobbleverse 1.7.42
- Cobblemon 1.7.3
- Fabric API 0.116.14+1.21.1
- GeckoLib 4.9.2
- Java 21

## Objetivo

Emipokemon concentrará los sistemas propios del servidor relacionados con Pokémon, economía especial, gacha, casino, apuestas, recompensas y presentación 3D/animada. No pretende reemplazar mods administrativos maduros como LuckPerms, Flan, TAB o TPA.

## Estado

- Candidata actual: `0.4.0-alpha.65`, con reparación de tickets del pase, tutorial de Minecraft, guía de economía, XP de pase por trabajos, retratos Cobblemon y modelos de casino ampliados.
- Candidata actual de hologramas: `0.4.0-alpha.26`. El motor visual nuevo usa `minecraft:text_display` vanilla; `HologramEntity` queda solo para migrar entidades de alpha.20-alpha.25.
- `alpha.26` valida primero texto plano. Placeholders y Streamotes permanecen temporalmente fuera del holograma hasta confirmar visualmente el texto en Cobbleverse real.
- Fase 1 — Core: validada.
- Fase 2 — Backend del Gacha: núcleo y economía por ticket validados en Cobbleverse.
- Fase 3 — Gacha 3D: validada en Cobbleverse y publicada como `0.3.0` estable.
- Fase 4 — Experiencia social y progresión: `0.4.0-alpha.19` compilada y validada en servidor Cobbleverse 1.7.42; queda en pausa para la prueba manual visual y de combate.
- El Hub físico será construido por la propietaria del servidor. Emipokemon conserva sus comandos y protecciones, pero no generará ni reemplazará la construcción.

## Cambios principales de alpha.19

- Corregidos los colores intercambiados de PNG/GIF y el oscurecimiento ambiental de paneles.
- Corregida la sincronización y renovación inmediata de skins de NPC personalizados.
- Editor ingame de nombre, diálogo, equipo Pokémon, skin, imagen/GIF y tamaño del panel.
- Subida directa de PNG/GIF desde el cliente al servidor, además de URL HTTPS.
- Combate contra equipos de hasta seis Pokémon mediante las APIs exactas de RCT de Cobbleverse.
- Persistencia de diálogo y equipo comprobada tras reinicio completo del servidor.

## Contenido conservado desde alpha.18

- El diario divide 49 misiones en **Minecraft** (11), **Pokémon** (29) y **Aventura** (9). Estructuras especiales y altares pertenecen a Aventura y mantienen progreso independiente.
- Los ocho líderes de Kanto usan exclusivamente sus avances oficiales `cobbleverse:trainer/kanto/defeat_*`; se eliminó la detección aproximada por nombre visible.
- NPC personalizados clásicos o slim con skin PNG desde carpeta del servidor o URL HTTPS.
- Paneles persistentes para PNG y GIF desde carpeta del servidor o URL HTTPS, con tamaño y posición administrables.
- Archivos limitados a 4 MiB, imágenes a 2048×2048, GIF a 160 fotogramas y bloqueo de URLs locales/privadas.
- Formato de datos `4` y todos los IDs de misión anteriores conservados para no reiniciar progreso.

## Contenido especial conservado desde alpha.17

- Equipo Rocket: entrada exacta a `cobbleverse:team_rocket_tower` y derrota RCT exacta de `team_rocket_giovanni`.
- Alto Mando: Liga Kanto, Lorelei, Bruno, Agatha, Lance y Blue mediante estructuras/avances oficiales.
- Estructuras: Torre Campana, Pilar Celeste y Columna Lanza mediante IDs registrados.
- Altares: invocación aceptada y captura exacta de Articuno, Zapdos y Moltres con LumyMon 0.6.6.
- Progreso, comprobación y recompensas totalmente autoritativos en servidor, con reclamación persistente de una sola vez.
- Auditoría detallada en `docs/COBBLEVERSE-1.7.42-AUDITORIA-MISIONES.md`.

## Progresión, Poké Mart y NPC de alpha.16

- Moneda virtual **Michicoins**, saldo por UUID y auditoría de transacciones.
- Ganancias por actividad real, capturas, combates, evoluciones y siete trabajos.
- Ganancias sin límite diario, con detección anti-AFK, validación de acciones y auditoría de transacciones.
- Varios trabajos simultáneos: Michi 2; MichiVIP/MichiDonador 4; MichiMod/MichiDueña los 7.
- Diario visual mediante el libro del inventario, tecla `J` o `/misiones`, dedicado exclusivamente a Misiones y Trabajos.
- Diario nítido compatible con shaders, botones decorados y accesos de misiones/Michicoins con iconos de gatito redibujados.
- Campaña secuencial desde el inicial hasta los ocho líderes de Kanto.
- Poké Mart visual de solo compra mediante `/tienda` o `/pokemart`, separada del diario, con 54 productos en nueve categorías.
- Incluye Poké Balls y evoluciones adicionales, cuatro protecciones públicas de EmiProtecciones y únicamente el ticket normal del gacha.
- La Protección Emi de 121×121 está excluida del catálogo y bloqueada en el servidor; ningún jugador, permiso u operador puede comprarla.
- Precios, cantidades máximas, descuentos VIP/Donador y confirmación de compras caras configurables en `config/emipokemon/shop/pokemart.json`.
- Compras verificadas por el servidor, con comprobación de saldo e inventario, bloqueo de doble clic y registro en el historial de Michicoins.
- Vendedor persistente con skin propia que abre la Poké Mart completa al interactuar.
- Enfermera persistente con skin slim propia que restaura vida, estados y PP de todo el equipo Pokémon.
- Administración ingame de NPC: crear, mover, eliminar, renombrar, elegir categoría inicial y listar.
- Los líderes se detectan por los avances oficiales exactos auditados en el datapack real de Cobbleverse; no se compara texto ni nombre visible.
- Este sistema pertenece únicamente a Servidor Emili y no integra Bingo, SkyWars ni sistemas Arlight.

## Dependencia opcional para emotes

El Hub y el gacha funcionan sin Streamotes. El botón visual **Emotes** requiere en cliente y servidor:

- Streamotes `1.2.12+1.21`.
- YetAnotherConfigLib `3.6.6+1.21.1-fabric`.

Emipokemon añade `emiilyextacy`, retira los tres canales de ejemplo de Streamotes y conserva cualquier canal añadido manualmente. El build alpha.18 fue validado con Fabric Loader `0.18.4`, el loader exacto de Cobbleverse 1.7.42.

Desde `0.4.0-alpha.7`, Emipokemon conserva los emotes ya descargados entre sesiones usando sus IDs de proveedor. Al entrar revisa el catálogo sin bloquear el renderizado: los IDs existentes se reutilizan desde disco y Streamotes solo descarga los emotes nuevos. Los archivos eliminados del catálogo se conservan durante 30 días y la caché se limita automáticamente a 256 MiB.

La configuración sincronizada exige códigos `:emote:` completos, conserva el color original de las imágenes y carga solo paquetes de canal. Los emotes ya no sustituyen letras dentro de nombres o palabras.

Consulta `ROADMAP.md` para el plan completo.
