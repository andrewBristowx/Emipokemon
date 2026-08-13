# Emipokemon 0.4.0-alpha.63

- Sustituye los peluches ficticios dibujados en la interfaz por ranuras que muestran los ítems reales `pokeblocks:pokedoll_*` enviados por el servidor.
- Convierte la garra en un minijuego interactivo: mover a izquierda/derecha, bajar la garra y recoger el Pokédoll seleccionado.
- Hace que el servidor valide el premio, consuma exactamente un `claw_ticket` y persista la operación antes de entregar el ítem.
- Añade recuperación de operaciones confirmadas tras reinicio y devolución del ticket si el premio deja de existir.
- Corrige la textura de `claw_ticket`: icono pixel art transparente, sin rectángulo blanco.
- Rediseña la pantalla de cara o sello Pokémon para mostrar el Pokémon elegido por cada jugador, su nivel y su estado dentro de los marcos laterales.
- Recoloca textos, rondas y controles de cara o sello para que no invadan los paneles.
- Diferencia los modelos del mundo: la garra ahora es un gabinete alto de dos bloques con vitrina, carril, garra y cajón; cara o sello usa una mesa baja con puestos enfrentados y moneda central.
- Rechaza que ambos lados depositen el mismo UUID Pokémon y amplía la auditoría de confirmación, depósito, resultado, entrega y recuperación.
- Conserva la configuración y los datos de `0.4.0-alpha.62`; no mezcla fuentes ni recursos de Arlight.

Esta versión sigue necesitando una compilación con Java 21 y una prueba manual en servidor con dos cuentas antes de considerarse validada para producción.
