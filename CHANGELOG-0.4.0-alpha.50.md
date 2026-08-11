# Emipokemon 0.4.0-alpha.50

Corrección visual de la ruleta basada en la validación real de alpha.49.

- Ajusta la composición por ancho y alto conservando la proporción 3:2 original.
- Restaura el divisor central, el pie completo y los adornos inferiores desde la referencia exacta aportada por el proyecto.
- Reduce automáticamente la interfaz en pantallas anchas para que ningún borde quede fuera del área visible.
- Mantiene la rueda dentro de su zona y evita que tape la cabecera o el tapete.
- Fija los números a la corona estática y elimina las placas rectangulares superpuestas.
- Evita dibujar una segunda bola encima de la bola incluida en el arte.
- Cubre por completo el contador `0/8` horneado y muestra una sola cuenta real enviada por el servidor.
- No cambia apuestas, pagos, reservas, persistencia ni protecciones contra duplicación.

La compilación no sustituye la validación visual real en Cobbleverse.
