# Emipokemon 0.4.0-alpha.61

- Añade sonidos locales y limitados para botones, apuestas, ruleta, tragamonedas, cartas, dados, cambio de fichas y tickets.
- Añade una señal diferenciada al finalizar el resultado y una celebración local cuando el mensaje del servidor confirma un premio.
- Evita repetir un sonido cada fotograma mediante pasos temporizados y banderas de una sola ejecución.
- Conserva el cronómetro visual cuando el servidor actualiza la pantalla de una misma fase.
- Corrige la lectura del resultado de dados: ahora toma `dado + dado` del estado público autoritativo y no del texto privado de premio/derrota.
- Refuerza la animación de dados a 2,6 segundos con caras alternas, rebote, giro, desplazamiento y pulso; termina en los valores exactos enviados por el servidor.
- No modifica probabilidades, apuestas, saldos, premios, persistencia ni lógica autoritativa del casino.

Esta versión requiere una validación audiovisual real dentro de Cobbleverse antes de considerarse estable.
