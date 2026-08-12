# Guía de pruebas — 0.4.0-alpha.69

Instala el mismo JAR alpha.69 en servidor y clientes. Retira alpha.68 antes de iniciar.

## Display físico

1. Coloca una máquina normal y una de Emi y obsérvalas desde frente, laterales y diagonales.
2. Confirma que ambas muestran `POKÉMON DE TEMPORADA` y el nombre mirando al jugador.
3. Comprueba Rayquaza y, si es posible, otras especies largas, grandes, pequeñas y voladoras; ninguna debe estirarse ni tapar el texto.
4. Rodea ambas máquinas y confirma que no desaparecen paneles/caras por el ángulo.
5. La normal mantiene destellos suaves; Emi mantiene polvo rosa y corazones.

## Interfaz

1. Abre normal y Emi en varias resoluciones.
2. Ejecuta x1 y x10.
3. En x10, los resultados deben aparecer uno a uno con un pequeño pop y sonidos; no deben mostrarse los diez de golpe.
4. Ningún retrato puede invadir las líneas de nombre, rareza o nivel, incluso con Pokémon grandes o largos.
5. Comprueba que los botones, tickets, pity y títulos siguen dentro de los marcos originales.

## Rotación de Emi

1. Confirma que el Pokémon mostrado en la máquina Emi coincide con el nombre del banner visual de Emi.
2. Revisa `config/emipokemon/gacha/emi_featured_rotation.json`; debe guardar la ventana y especie activa.
3. El pool de Emi debe construirse con todos los Pokémon implementados cuyo catálogo Emipokemon clasifique como `LEGENDARY`.
4. La ventana es de 12 horas reales; reiniciar dentro de la misma ventana debe conservar el mismo legendario.
5. Al cambiar de ventana debe seleccionarse otro legendario cuando haya más de una opción disponible.
6. El pity acumulado no debe reiniciarse al rotar.
7. La máquina normal debe seguir realizando tiradas aleatorias sin el multiplicador temático de Emi.

## Regresión obligatoria

- Fondos normal/Emi idénticos a alpha.68.
- Texturas físicas normal/Emi idénticas a alpha.68.
- Sin `reveal_sheet.png` ni video descartado.
- Backend autoritativo, entrega equipo/PC, tickets, pity, casino, kits, pase, diario, Michicoins, paneles administrativos y LuckPerms continúan funcionando.
- Probar las cinco herramientas de Emi según la guía alpha.68.
