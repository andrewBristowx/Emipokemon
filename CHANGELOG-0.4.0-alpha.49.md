# Emipokemon 0.4.0-alpha.49

- Parte exclusivamente del ZIP fuente verificado de `0.4.0-alpha.48`.
- Elimina los huecos negros internos haciendo que ambos paneles HD ocupen todo el ancho de la cabecera.
- Recupera el borde inferior y sus adornos originales; ya no se repintan con franjas planas.
- Superpone correctamente la cabecera sobre la unión de paneles para ocultar restos recortados.
- Dibuja la cantidad una sola vez manteniendo el `TextFieldWidget` real para foco y teclado.
- Integra Michicoins, ronda, temporizador, últimos resultados y contador en sus zonas reservadas.
- El contador usa `state.players()` enviado por el servidor.
- Añade capacidad autoritativa de ocho participantes a la ruleta antes de reservar saldo.
- No modifica premios, resolución compartida, auditoría, protección anti-duplicación ni protocolo de acciones.
