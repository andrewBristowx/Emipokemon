# Guía de pruebas — 0.4.0-alpha.68

Instala el mismo JAR en servidor y clientes y retira alpha.67 antes de iniciar.

## Herramientas

Entrega el set como operador:

```mcfunction
/give @s emipokemon:emi_sword
/give @s emipokemon:emi_pickaxe
/give @s emipokemon:emi_axe
/give @s emipokemon:emi_shovel
/give @s emipokemon:emi_hoe
```

1. Comprueba que el pico y la pala rompen únicamente bloques compatibles en una cuadrícula 3×3.
2. Repite agachado: sólo debe romperse el bloque apuntado.
3. Tumba un árbol natural con hojas y después prueba una construcción de troncos sin hojas.
4. Prueba el 3×3 y el talado dentro y fuera de una protección; cada bloque debe respetar el permiso normal.
5. Ara una zona amplia y comprueba que los minerales aparecen ocasionalmente, no en cada bloque.

## Gacha

1. Abre las máquinas normal y Emi con resoluciones distintas.
2. Comprueba títulos, tickets, pity y botones dentro de sus marcos.
3. Ejecuta una tirada x1 y una x10; verifica retrato, nombre, rareza y nivel sin superposición.
4. Configura un banner con `featuredSpecies` y asígnalo a la máquina.
5. Confirma que el Pokémon y su nombre aparecen sobre la máquina.
6. La máquina normal debe emitir destellos; la de Emi debe emitir polvo rosa y corazones.

## Regresión

- Las texturas físicas y los dos fondos del gacha deben verse iguales a alpha.67.
- El video descartado no debe reaparecer.
- Kits, pase, diario, casino y grupos LuckPerms deben conservar sus datos.
