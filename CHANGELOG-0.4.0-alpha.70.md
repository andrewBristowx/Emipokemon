# Emipokemon 0.4.0-alpha.70

Base: `0.4.0-alpha.69`. Esta versión corrige únicamente problemas encontrados durante la prueba real de alpha.69 y conserva alpha.69 intacta.

## Gacha — resultados

- Los retratos x10 recuperan un tamaño legible (`82` unidades escaladas) sin perder el recorte seguro que impide invadir nombre, rareza y nivel.
- El retrato x1 vuelve al tamaño grande aprobado de `190` unidades escaladas.
- Se conserva la revelación escalonada y los sonidos introducidos en alpha.69.

## Gacha — probabilidades

- El rate base combinado de LEGENDARY + MYTHICAL baja de 5 % a 1 % antes de pity:
  - LEGENDARY: 0,8 %.
  - MYTHICAL: 0,2 %.
- COMMON/UNCOMMON/RARE/EPIC se rebalancean a 52/27/14/6.
- Los banners antiguos que conservan exactamente los pesos por defecto 45/27/15/8/4/1 migran automáticamente al nuevo reparto.
- Los rates personalizados por el administrador no se sobrescriben.
- Pity épico, soft pity y hard pity legendario mantienen su lógica anterior.

## Display físico de temporada

- Se elimina la carga de un Pokémon desde NBT incompleto, causa de que alpha.69 dejara de mostrar el modelo destacado.
- La especie se resuelve por el registro real de Cobblemon y se aplica al `PokemonEntity` antes de renderizarlo.
- Texto y modelo se renderizan de forma independiente; el texto se intenta dibujar aunque falle el modelo.
- Las máquinas con datos destacados vacíos refrescan el spotlight inmediatamente en el siguiente tick de servidor.
- Continúa la rotación aleatoria de legendarios de Emi cada 12 horas y el spotlight aleatorio visual del gacha normal.

## Máquina de garra

- Se añade una carcasa lateral estructural que une los paneles existentes con la base y el techo.
- Se cierran las uniones delanteras y traseras que dejaban huecos visibles entre los lados del gabinete.
- No se reemplazan las texturas de la máquina; la corrección es exclusivamente geométrica.

## Regresión protegida

- Los fondos aprobados `standard_gacha.png` y `emi_gacha.png` permanecen idénticos byte por byte.
- Las texturas físicas de las máquinas gacha normal y Emi permanecen idénticas byte por byte.
- `reveal_sheet.png` continúa ausente.
- Se conservan backend, pity, tickets, rotación Emi, casino, herramientas, kits, pase, diario, Michicoins, LuckPerms y autoridad del servidor.
