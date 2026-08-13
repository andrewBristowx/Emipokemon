# Instalación y pruebas de 0.4.0-alpha.64

## Antes de instalar

1. Detén cliente y servidor y respalda `config/emipokemon`, el mundo y los datos de jugador.
2. Sustituye solamente el JAR anterior de Emipokemon en cliente y servidor.
3. Mantén Minecraft 1.21.1, Java 21, Fabric Loader 0.18.4, Fabric API 0.116.14, Cobblemon 1.7.3, GeckoLib 4.9.2 y Pokeblocks 1.4.0 o posterior.
4. No borres la configuración: al iniciar debe migrar automáticamente a versión 6.

## Qué valida la compilación automática

- Compilación de código principal y cliente con Java 21.
- Pruebas JUnit existentes y regresiones nuevas de persistencia, pools, pase, XP, cartera virtual y render Pokémon.
- Presencia y tamaño exacto de los fondos gráficos.
- Empaquetado de clases, modelos, traducciones y texturas dentro del JAR.

Estas pruebas no reemplazan las comprobaciones visuales y multijugador siguientes.

## Recompensa diaria

1. Entra con un jugador sin reclamación del día: tras unos dos segundos debe abrirse el diario; `/diario` también debe abrirlo.
2. Comprueba que el fondo, las siete muestras y el panel derecho caben sin solaparse a escala GUI 2, 3 y 4.
3. Pulsa reclamar: debe aparecer la animación gacha, revelar un único premio y desactivar el botón.
4. Cierra y vuelve a abrir el menú el mismo día: no debe existir una segunda reclamación.
5. Reinicia el servidor y reconecta: el día, racha, total y último premio deben conservarse.
6. Prueba con inventario lleno: los objetos restantes deben caer junto al jugador sin perderse.
7. Fuerza cada tipo del pool temporalmente en `config/emipokemon/emipokemon.json` y verifica Michicoins, diamantes, objetos Cobblemon, tirada estándar, ticket de garra, tirada Emi y Pokémon.
8. Para el Pokémon, llena equipo y PC según la prueba controlada de Cobblemon y confirma que la entrega no se duplica.
9. Cambia la fecha únicamente en un servidor de pruebas: la racha debe subir en días consecutivos y reiniciarse después de saltar uno o más días.
10. Revisa `config/emipokemon/daily-reward-operations` y `daily-reward-audit.log`; cada reclamación debe terminar en `DELIVERED`.

## Pase infinito

1. Abre `/pase` o pulsa `P`: deben verse ocho niveles, la fila gratuita arriba y la premium abajo.
2. Cambia de página hacia adelante y atrás; los niveles deben continuar sin un final fijo.
3. Gana XP mediante tiempo activo, reclamación de misión, captura, nueva especie, evolución, victoria, bioma nuevo y nivel de trabajo. Comprueba que cada acción actualiza la barra.
4. Captura repetidamente en una prueba controlada: después del máximo configurable de eventos por minuto no debe concederse más XP hasta la siguiente ventana.
5. Como jugador sin rango premium, alcanza el nivel 4: reclama una tirada Emi gratuita y confirma que la fila inferior permanece bloqueada.
6. Como VIP, Donador, Mod, Moderador, Emi u operador, reclama diez tiradas Emi en nivel 1 y dos en nivel 4.
7. Otorga el rango premium después de haber avanzado varios niveles: las recompensas premium anteriores deben quedar reclamables.
8. Retira el rango tras reclamar: no debe duplicarse ninguna recompensa ni borrarse la marca de reclamo.
9. Reinicia el servidor: XP, nivel, créditos y reclamos deben conservarse.
10. Revisa `battle-pass-audit.log` para confirmar cada ganancia y reclamación.

## Tiradas virtuales

1. Concede o reclama una tirada Emi en el pase y abre la máquina Emi sin tickets físicos.
2. La máquina debe aceptar la tirada, descontar exactamente un crédito y entregar exactamente un Pokémon.
3. Si también existe ticket físico, primero debe consumirse el crédito virtual.
4. Provoca un fallo de entrega controlado: el crédito virtual debe devolverse.
5. Reinicia y confirma que el saldo restante se conserva.

## Casino y modelos

1. Coloca `emipokemon:claw_machine`: la colisión debe cubrir el gabinete alto y su marquesina, sin impedir el uso normal del frente.
2. La interfaz de garra no debe mostrar saldo, apuesta ni texto de Michicoins; sus ranuras deben mostrar Pokédolls reales.
3. Coloca `emipokemon:pokemon_wager_table`: debe ser una mesa baja distinta de la garra, con dos estaciones y moneda central.
4. Entra con dos cuentas, selecciona Pokémon diferentes y confirma ambos lados.
5. Cada marco debe mostrar el retrato real de la especie elegida, nombre, especie y nivel. Un icono de Poké Ball indica fallo y bloquea la aprobación de esta versión.
6. La pantalla de cara o sello no debe mostrar saldo o apuesta en Michicoins.
7. Termina el mejor de tres y confirma que el ganador recibe ambos Pokémon, usando PC cuando el equipo esté lleno.
8. Verifica ángulos norte, sur, este y oeste, iluminación nocturna y un cliente con shaders si el servidor los recomienda.

## Criterio de aprobación

La candidata se puede promover solo cuando el flujo Java 21 esté verde y las pruebas reales de diario, pase, reinicio y casino con dos cuentas se completen sin pérdida, duplicación, Poké Balls de reemplazo ni solapamientos visuales.
