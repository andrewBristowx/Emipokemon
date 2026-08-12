# Instalación y pruebas de Emipokemon 0.4.0-alpha.54

## Instalación

1. Apaga completamente el cliente y el servidor.
2. Haz una copia de seguridad del mundo, `config/emipokemon` y los datos del servidor.
3. Retira el JAR anterior de Emipokemon de las carpetas `mods` del cliente y del servidor. No dejes dos versiones juntas.
4. Copia `emipokemon-0.4.0-alpha.54.jar` en ambas carpetas `mods`.
5. Conserva mundos y configuraciones existentes; esta versión no requiere borrarlos.
6. Inicia servidor y cliente con Java 21.

## Validación real obligatoria

1. Abre la ruleta y confirma que no exista un logo fijo duplicado en el centro.
2. Revisa el saldo, Tu ficha, Jugadores y Mesa siguiendo `GUIA-PRUEBA-VISUAL.md`.
3. Prueba sin apuesta, con apuesta exacta y con apuesta exterior.
4. Ejecuta una ronda con dos clientes: ambos deben ver el mismo resultado y el contador debe cambiar de 0/8 a 1/8 o 2/8 según corresponda.
5. Verifica que la apuesta se cobre una vez y la recompensa se entregue una vez.
6. Cierra y abre la interfaz, reconecta ambos clientes y reinicia el servidor; no deben duplicarse apuestas ni pagos.
7. Repite con shaders activados y desactivados y con escala GUI automática.

No promociones esta compilación a estable hasta completar esta validación. Si algo no coincide, guarda una captura completa y `latest.log`.
