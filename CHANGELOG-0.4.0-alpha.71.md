# Emipokemon 0.4.0-alpha.71

Base exclusiva: `0.4.0-alpha.70`.

## Correcciones visuales verificadas a partir de capturas alpha.70

- Corrige los retratos vacíos del gacha x1/x10 usando la API real de Cobblemon 1.7.3: `com.cobblemon.mod.common.api.gui.GuiUtilsKt.drawProfile` con `FloatingState`.
- Mantiene el recorte seguro de las tarjetas para que criaturas grandes no invadan nombre, rareza o nivel.
- Añade `SeasonalDisplayPayload` S2C con especie y nombre destacados para que el cliente no dependa únicamente del último paquete NBT del block entity.
- Reenvía el display destacado periódicamente a jugadores cercanos y también cuando cambia el estado de la máquina.
- El renderer mundial muestra el encabezado incluso mientras llega la sincronización y mueve el conjunto por encima de la carcasa física real.
- Pokémon destacado: base visual elevada de Y 2.62 a Y 2.78.
- Texto destacado: elevado desde Y 2.23 (dentro del gabinete alto) hasta Y 4.18.
- Cierra juntas de la máquina de garra mediante una nueva pieza `alpha71_sealed_joints` de 10 cubos: fascia frontal inferior/superior, uniones traseras, suelo y cuatro conectores de esquina.

## Conservado

- Rates alpha.70: 0.8 % Legendary + 0.2 % Mythical de base.
- Pity, tickets, transacciones y selección autoritativa del servidor.
- Rotación del legendario de Emi cada 12 horas.
- Fondos aprobados de ambos gachas sin cambios.
- Texturas físicas de las máquinas gacha sin cambios.

## Validación pendiente

- `clean test build` Java 21 en GitHub Actions.
- Prueba visual real dentro de Cobbleverse antes de declarar estable.
