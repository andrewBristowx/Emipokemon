# Emipokemon 0.4.0-alpha.69

## Display físico de temporada

- El Pokémon destacado ya no usa el renderer de perfil de GUI dentro del mundo; ahora se dibuja como entidad Cobblemon con escala adaptativa según sus dimensiones.
- Se separó la rotación del Pokémon de la rotación del texto para que `POKÉMON DE TEMPORADA` y el nombre siempre miren correctamente al jugador.
- Se corrige la deformación extrema de especies largas como Rayquaza.
- El renderer de las máquinas usa translucencia sin back-face culling para evitar paneles/caras que parecían faltar según el ángulo.
- Las texturas físicas aprobadas no fueron modificadas.

## Gacha x1/x10

- Los retratos quedan recortados a un área segura y ya no pueden cubrir nombre, rareza ni nivel.
- Las tiradas x10 se revelan de forma escalonada (135 ms por resultado) con una animación corta de aparición.
- Se añadieron sonidos locales de pulsación y reveal, con feedback distinto para rarezas altas.
- Los fondos aprobados `standard_gacha.png` y `emi_gacha.png` permanecen idénticos byte por byte.
- No se restaura el video descartado ni `reveal_sheet.png`.

## Rotación de Emi

- El gacha de Emi selecciona aleatoriamente un legendario del catálogo real completo de tier `LEGENDARY`.
- La rotación cambia cada 12 horas reales, evita repetir inmediatamente cuando existen alternativas y persiste durante reinicios dentro de la misma ventana.
- El legendario activo se aplica a las probabilidades reales del banner de Emi con multiplicador destacado; no es sólo decoración.
- El pity sigue ligado al banner y no se reinicia cuando cambia el legendario.
- El gacha normal mantiene tiradas aleatorias sin multiplicador de destacado y usa un spotlight visual aleatorio.
- Las máquinas estándar nuevas usan el banner `standard`; las máquinas alpha.68 que conservaban implícitamente `rayquaza_hoenn` migran a `standard` salvo que el banner quede marcado como configurado explícitamente en alpha.69.

## Compatibilidad

- Minecraft 1.21.1, Fabric, Java 21, Cobblemon 1.7.3 y GeckoLib 4.9.2.
