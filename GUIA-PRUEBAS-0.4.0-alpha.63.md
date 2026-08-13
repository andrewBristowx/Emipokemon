# Instalación y pruebas de 0.4.0-alpha.63

## Antes de instalar

1. Usa el JAR construido por el flujo `alpha63-interactive-claw-pokemon-display-build` únicamente cuando todas sus comprobaciones estén verdes.
2. Detén cliente y servidor y respalda `config/emipokemon`, el mundo y los datos de jugador.
3. Sustituye solamente el JAR anterior de Emipokemon en cliente y servidor.
4. Mantén Minecraft 1.21.1, Java 21, Fabric Loader 0.18.4, Fabric API 0.116.14, Cobblemon 1.7.3, GeckoLib 4.9.2 y Pokeblocks 1.4.0 o posterior.

## Máquina de garra

1. Coloca `emipokemon:claw_machine`: debe verse como gabinete alto con vitrina y no como una mesa genérica.
2. Abre la interfaz: no debe haber criaturas ficticias pintadas en el fondo.
3. Comprueba que aparecen hasta cinco Pokédolls reales de Pokeblocks sobre las bases iluminadas.
4. Mueve la garra a izquierda y derecha; el selector y la garra deben seguir la posición elegida sin consumir ticket.
5. Pulsa bajar sin ticket: no debe entregar premio ni modificar el inventario.
6. Con un solo `emipokemon:claw_ticket`, baja la garra: debe consumirse exactamente uno y entregarse exactamente el Pokédoll mostrado en esa posición.
7. Repite con el inventario lleno: el premio restante debe caer de forma segura junto al jugador.
8. Revisa `config/emipokemon/claw-operations` y `config/emipokemon/casino-audit.log` para confirmar los estados `COMMITTED` y `DELIVERED`.
9. Interrumpe una prueba controlada después de confirmar una operación y reinicia: una operación pendiente debe completar la entrega o devolver el ticket si el premio ya no está registrado.

## Ticket de garra

1. Visualiza `emipokemon:claw_ticket` en inventario, mano y suelo.
2. Debe tener contorno pixel art limpio, transparencia real y ningún cuadro blanco.
3. Confirma que el ticket gacha normal no funciona en la garra.

## Cara o sello Pokémon — dos cuentas obligatorias

1. Coloca `emipokemon:pokemon_wager_table`: debe verse como mesa baja de moneda, distinta de la garra y de las demás mesas.
2. Entra con dos jugadores reales y selecciona un Pokémon diferente en cada lado.
3. Cada marco debe mostrar nombre de jugador, retrato del Pokémon elegido, especie, nivel y estado de confirmación sin textos superpuestos.
4. Intenta seleccionar el mismo UUID Pokémon desde ambos lados: la operación debe rechazarse.
5. Confirma ambos Pokémon y juega el mejor de tres; comprueba los indicadores de cara/sello y el resultado.
6. El ganador debe recibir ambos Pokémon; con equipo lleno, el excedente debe llegar al PC.
7. Reinicia durante una prueba controlada y verifica `config/emipokemon/pokemon-wager-escrow` y `casino-audit.log`: no debe perderse ni duplicarse ningún Pokémon.

## Criterio de aprobación

No publiques el JAR como versión estable hasta completar correctamente la compilación Java 21, las pruebas automatizadas y esta prueba manual cliente/servidor con dos cuentas.
