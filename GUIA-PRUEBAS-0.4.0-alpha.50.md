# Guía de validación — Emipokemon 0.4.0-alpha.50

## Preparación

1. Respaldar el mundo y retirar únicamente el JAR anterior de Emipokemon.
2. Instalar `emipokemon-0.4.0-alpha.50.jar` en cliente y servidor.
3. Mantener Java 21 y el resto de Cobbleverse sin cambios.

## Interfaz de ruleta

1. Abrir la ruleta en pantalla completa con la misma escala GUI usada en la captura de alpha.49.
2. Confirmar que existe margen oscuro uniforme alrededor de todo el marco.
3. Confirmar que el pie morado, el texto EMIPOKEMON y los adornos de ambas esquinas inferiores aparecen completos.
4. Confirmar que la rueda no invade la cabecera ni tapa la primera fila del tapete.
5. Confirmar que cada número queda alineado con una posición de la corona y solo hay una bola blanca.
6. Confirmar que no aparece ningún resto del `0/8` original detrás del contador real.

## Funciones autoritativas

1. Cambiar la cantidad con las flechas y con Min, x5 y x10.
2. Apostar a un número, color, paridad, docena y columna.
3. Comprobar que el servidor descuenta una sola vez y rechaza una segunda apuesta en la misma ronda.
4. Entrar con dos clientes y verificar que el contador muestre el número real de participantes.
5. Completar varias rondas y comprobar que los resultados reales aparecen en “Últimos resultados”.
6. Reiniciar servidor y volver a validar saldo, persistencia y ausencia de premios duplicados.
