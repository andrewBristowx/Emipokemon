# Instalación y pruebas de 0.4.0-alpha.62

## Instalación

1. Detén cliente y servidor.
2. Conserva una copia de `config/emipokemon`, mundos y archivos de jugador.
3. Sustituye únicamente el JAR anterior de Emipokemon por `emipokemon-0.4.0-alpha.62.jar` en cliente y servidor.
4. Verifica Java 21, Fabric Loader 0.18.4, Cobblemon 1.7.3 y Pokeblocks 1.4.0.
5. Arranca primero el servidor y después el cliente.

## Máquina de garra

1. Obtén `emipokemon:ticket_machine` y `emipokemon:claw_machine`.
2. Compra un ticket: debe recibirse `emipokemon:claw_ticket`, nunca `emipokemon:gacha_ticket`.
3. Usa la garra sin ticket: no debe entregar premio.
4. Usa la garra con un ticket: debe consumir exactamente uno y entregar exactamente un `pokeblocks:pokedoll_*`.
5. Repite con inventario casi lleno y revisa `config/emipokemon/casino-audit.log`.

## Cara o sello Pokémon — prueba obligatoria con dos cuentas

1. Coloca `emipokemon:pokemon_wager_table` y entra con dos jugadores reales.
2. Cada jugador se une, recorre su equipo y confirma un Pokémon.
3. Antes de la segunda confirmación no debe retirarse ningún Pokémon.
4. Tras el mejor de tres, el ganador debe conservar el suyo y recibir el del rival; si su equipo está lleno, debe llegar al PC.
5. Reinicia el servidor durante una prueba controlada y verifica que `config/emipokemon/pokemon-wager-escrow` restaure o complete una transacción sin UUID duplicados.
6. No uses Pokémon valiosos hasta completar esta validación con copias de seguridad.

## Pagos

- Apuesta 10 a una opción 2×: saldo neto final +10 al ganar (se acreditan 20 totales).
- Empate de blackjack/póker: devuelve 10, beneficio neto 0.
- En póker, un segundo jugador con importe distinto debe ser rechazado antes del cobro.
