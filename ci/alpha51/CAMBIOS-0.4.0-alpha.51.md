# Emipokemon 0.4.0-alpha.51

Versión candidata centrada únicamente en la presentación e interacción de la ruleta.

## Cambios

- Sustituye las capas superpuestas de `alpha.50` por una sola composición estática de 1536×1024.
- Usa una rueda separada con transparencia real; sus 37 números se generan desde el orden europeo exacto del código.
- Recupera el giro de la rueda y añade una bola independiente que termina sobre el resultado compartido por el servidor.
- Convierte la mesa, fichas rápidas, flechas de cantidad y cierre en zonas de clic transparentes alineadas con la imagen.
- Integra saldo, apuesta, fase, temporizador, ficha elegida, jugadores, estado de mesa e historial en espacios reservados del diseño.
- Muestra el contador real de participantes con capacidad `n/8`.
- Incorpora Pixelify Sans bajo SIL Open Font License para que los datos dinámicos mantengan una apariencia pixelada coherente.
- Conserva sin cambios el cálculo de resultados, apuestas, saldos, persistencia y protecciones autoritativas del servidor de `alpha.50`.

## Validación automática

- Java 21.
- `clean test build` completado.
- 76 pruebas ejecutadas, 0 fallas y 0 errores.
- JAR remapeado comprobado con fondo, rueda RGBA, fuente y licencia incluidas.

## Pendiente antes de declarar estable

- Validación visual real en Cobbleverse con diferentes escalas de GUI y con/sin shaders.
- Comprobación multijugador de que la bola, el historial y el pago muestran el mismo resultado del servidor.
