# Instalación y prueba de Emipokemon 0.4.0-alpha.49

## Instalación

1. Detén el servidor y cierra todos los clientes.
2. Conserva una copia del JAR `alpha.48`.
3. Sustituye solamente el JAR de Emipokemon por `emipokemon-0.4.0-alpha.49.jar` en servidor y clientes.
4. No borres configuraciones, mundos ni datos persistentes.
5. Inicia primero el servidor y revisa que el registro muestre `0.4.0-alpha.49` sin errores.
6. Inicia Cobbleverse y abre una mesa de ruleta.

## Validación visual real

- Probar con escala de GUI Automática, 2 y 3 en 1920×1080.
- Confirmar que no hay huecos negros entre la cabecera y los paneles.
- Confirmar que no aparecen trozos del logo o bordes debajo de la cabecera.
- Revisar que el borde inferior, monedas, fichas y adornos estén completos.
- Confirmar que Cantidad aparece una sola vez, admite clic, teclado, borrar y escribir.
- Probar Min, ×5, ×10, flechas y todas las casillas del tapete.
- Verificar que Michicoins no tape la moneda ni salga de su cápsula.
- Confirmar que Ronda y el temporizador permanecen dentro de su sección.
- Completar varias rondas y comprobar hasta cinco resultados en Últimos resultados.
- Confirmar que Tu ficha y Mesa no cubren encabezados ni bordes.

## Validación multijugador y servidor

1. Abre la misma ruleta con dos clientes.
2. El primer cliente apuesta: ambos deben mostrar `1/8` y el mismo participante.
3. El segundo cliente apuesta: ambos deben mostrar `2/8`.
4. Comprueba que el resultado y el historial son iguales en ambos clientes.
5. Con ocho participantes debe verse `8/8`.
6. Una novena apuesta debe ser rechazada antes de descontar Michicoins.
7. Reinicia durante una ronda de prueba y comprueba las protecciones y devoluciones existentes.
8. Revisa `casino-audit.log` y confirma que no hay pagos o reservas duplicados.

La compilación automatizada no sustituye esta validación dentro del modpack real con sus shaders y escala de GUI.
