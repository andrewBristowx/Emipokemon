# Instalación y pruebas de Emipokemon 0.4.0-alpha.51

## Instalación

1. Apaga completamente el cliente y el servidor.
2. Haz una copia de seguridad del mundo, `config/emipokemon` y los datos del servidor.
3. Retira el JAR anterior de Emipokemon de las carpetas `mods` del cliente y del servidor. No dejes dos versiones juntas.
4. Copia `emipokemon-0.4.0-alpha.51.jar` en ambas carpetas `mods`.
5. Conserva todos los mundos y configuraciones existentes; esta versión no requiere borrarlos.
6. Inicia el servidor y el cliente con Java 21.

## Pausa de validación real

No promociones esta compilación a versión estable hasta completar lo siguiente:

1. Abre cada mesa de ruleta y confirma que no aparecen paneles, bordes, cantidades o títulos duplicados.
2. Comprueba el encuadre a escala de GUI automática, pequeña, mediana y grande. El marco inferior y los laterales deben quedar completos.
3. Repite la prueba con shaders activados y desactivados.
4. Pulsa las flechas de cantidad y los botones `Min`, `x5` y `x10`; el valor debe cambiar una sola vez por clic.
5. Pulsa el 0, varios números, rojo/negro, par/impar, docenas y columnas. La selección enviada debe corresponder a la casilla visible.
6. Haz una ronda con dos clientes. Ambos deben ver el mismo número, la bola debe terminar en él y el pago debe corresponder al resultado del servidor.
7. Verifica que `Jugadores` muestre el número real de participantes y que `Últimos resultados` conserve hasta cinco resultados compartidos.
8. Cierra y vuelve a abrir la interfaz: no debe duplicarse una apuesta ni un pago.
9. Reinicia servidor y cliente y confirma que mundos, saldos y configuraciones anteriores siguen intactos.

Si algo visual no coincide, guarda una captura completa, la escala de GUI, el shader usado y `latest.log`. Esa evidencia es necesaria para el siguiente ajuste.
