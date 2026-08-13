# Guía de pruebas — Emipokemon 0.4.0-alpha.70

Instala **el mismo JAR alpha.70 en servidor y clientes** y retira alpha.69 antes de iniciar. No borres configuraciones ni progreso para poder validar las migraciones reales.

## 1. Retratos del gacha

1. Abre el gacha normal y el de Emi.
2. Ejecuta varias tiradas x1 y x10.
3. En x10, comprueba que cada Pokémon se ve claramente más grande que en alpha.69.
4. Confirma que ningún retrato invade el nombre, la rareza ni el nivel.
5. Confirma que los diez resultados siguen apareciendo escalonados y mantienen sus sonidos.

## 2. Rates y pity

1. Conserva los JSON de banners usados en alpha.69 para probar la migración.
2. Reinicia y comprueba que un banner con los pesos antiguos por defecto usa el nuevo reparto 52/27/14/6/0.8/0.2.
3. Si tienes un banner con pesos personalizados, verifica que estos no cambian.
4. Realiza una muestra grande de tiradas de prueba: fuera de pity, legendarios y míticos deben ser notablemente menos frecuentes que en alpha.69.
5. Verifica que el pity épico y legendario sigue avanzando y garantizando en los mismos umbrales configurados.

## 3. Pokémon de temporada sobre las máquinas

1. Coloca una máquina normal y una máquina de Emi nuevas.
2. Espera unos segundos como máximo: ambas deben obtener species/nombre destacado sin intervención manual.
3. Confirma que aparece `POKÉMON DE TEMPORADA`, el nombre y el Pokémon sobre cada máquina.
4. Prueba especies de tamaños distintos, especialmente una larga/grande como Rayquaza.
5. Rodea la máquina y confirma que el texto sigue siendo legible y el modelo no se deforma.
6. Emi mantiene partículas rosas/corazones; normal mantiene destellos.
7. Reinicia el servidor dentro de la misma ventana de 12 horas: el legendario de Emi debe persistir.

## 4. Máquina de garra

1. Coloca una máquina de garra y revísala de frente, atrás, ambos lados y desde abajo/arriba.
2. Confirma que los laterales llegan visualmente desde la base hasta el techo.
3. Comprueba que ya no hay huecos entre los marcos laterales y las caras delantera/trasera.
4. Abre y usa la máquina para verificar que la geometría adicional no altera la interacción ni la garra animada.

## 5. Regresión

- Fondos normal/Emi del gacha: sin cambios visuales.
- Texturas físicas de las máquinas gacha normal/Emi: sin cambios.
- No debe existir `reveal_sheet.png`.
- Tickets, pity, entrega a equipo/PC, rotación Emi, casino, herramientas, kits, pase, diario, Michicoins y LuckPerms deben seguir funcionando.
