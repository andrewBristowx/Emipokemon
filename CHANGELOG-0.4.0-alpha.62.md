# Emipokemon 0.4.0-alpha.62

- Añade `claw_ticket`, separado del ticket gacha, con precio predeterminado de 250 Michicoins.
- La máquina de tickets entrega exclusivamente tickets de garra; el gacha conserva su ticket y precio anteriores.
- Añade máquina de garra: cada intento consume un ticket y entrega un Pokédoll validado del mod `pokeblocks`.
- Añade mesa de cara o sello Pokémon para dos jugadores y mejor de tres.
- El depósito Pokémon se persiste antes de retirar criaturas y se recupera por UUID tras reinicios sin duplicar.
- El ganador recibe su Pokémon y el del rival; la entrega desborda al PC cuando el equipo está lleno.
- Corrige el póker comunitario para exigir la misma entrada a todos los participantes.
- Audita pagos: los multiplicadores ganadores devuelven apuesta más ganancia; empates devuelven la apuesta.
- Añade fondos personalizados para la máquina de garra y la mesa Pokémon.
- Requiere Pokeblocks 1.4.0 o posterior para evitar una máquina sin premios.

La transferencia Pokémon necesita todavía una prueba manual real con dos cuentas antes de declarar la fase estable.
