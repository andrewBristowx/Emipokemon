# Emipokemon 0.4.0-alpha.67

Corrección visual y de rangos basada en las pruebas reales de `alpha.66`.

## Gacha

- Conserva los fondos de interfaz normal y Emi de `alpha.66`.
- Retira completamente la animación de video y su sprite sheet.
- Corrige los identificadores sin namespace (`deoxys` → `cobblemon:deoxys`) para renderizar los Pokémon obtenidos.
- Reordena nombre, tier y nivel en resultados x1 y x10.
- Centra y limita los textos de banner, estado y tiradas.

## Máquinas y casino

- Restaura exactamente los atlas físicos de máquinas y mesas de `alpha.65`.
- Mantiene centrados los iconos de Michicoin y Pokémon aleatorio.
- Completa la estructura superior interna de la máquina de garra con riel transversal y soporte central.
- Eleva retrato, nombre, nivel y estado en la mesa de Pokémon.
- Limita los textos de la mesa de dados a sus paneles decorados.

## LuckPerms

- Canoniza los grupos reales: `default`, `michiba`, `michivip`, `michidonador`, `michimod` y `michidueñas`.
- Trabajos: 2 para rangos normales, 4 para VIP/Donador y todos para Mod/Dueñas.
- Tienda, pase premium y kits usan los mismos nombres reales.
- Los kits aceptan nodos normales o `groups:grupo1,grupo2` desde el panel administrador.
- Migra los nombres predeterminados antiguos sin reemplazar permisos personalizados.
