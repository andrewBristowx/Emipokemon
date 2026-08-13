# Instalación y pruebas de 0.4.0-alpha.65

## Instalación

1. Detén cliente y servidor y respalda `config/emipokemon`, el mundo y los datos de jugador.
2. Sustituye el JAR anterior por alpha.65 tanto en cliente como en servidor.
3. Conserva Minecraft 1.21.1, Java 21, Fabric API 0.116.14, Cobblemon 1.7.3 y GeckoLib 4.9.2.
4. No borres la configuración; debe migrar automáticamente a versión 7.

## Pase y reparación de las diez tiradas

1. Entra con la cuenta que reclamó las diez tiradas en alpha.64.
2. Debe aparecer un mensaje indicando que sus tiradas guardadas se convirtieron en tickets físicos.
3. Comprueba diez `emipokemon:emi_special_banner_ticket` en inventario o, si estaba lleno, en el suelo junto al jugador.
4. Abre `/pase`: todos los niveles visibles deben mostrar icono, cantidad, nombre y estado.
5. Reclama un nivel gratuito y uno premium; el objeto debe aparecer físicamente y no duplicarse al volver a pulsar.
6. Comprueba que las zonas de flechas y X funcionan sin dibujar botones sobre el arte.

## Diario y tutorial

1. Pulsa `J` o usa `/misiones`: debe abrirse la pestaña Guía.
2. Revisa que Guía explique cómo obtener Michicoins, tickets, XP de pase y trabajos.
3. En Minecraft completa, en orden, las once misiones del tutorial.
4. Antes de reclamar, confirma que la pantalla muestra Michicoins, XP de pase y objetos.
5. Tras reclamar, confirma que suben el saldo y el pase exactamente por las cantidades anunciadas.
6. Las pestañas Pokémon y Aventura deben conservar sus misiones y progreso anteriores.
7. Reinicia el servidor y confirma que el progreso tutorial no retrocede.

## Trabajos y balance del pase

1. Activa Minero, Constructor u otro trabajo y realiza acciones válidas.
2. Cada 8 XP de trabajo acumulada debe concederse 1 XP de pase.
3. Una automatización rápida no debe superar 12 XP de pase por minuto mediante acciones de trabajo.
4. Subir un nivel de trabajo debe conservar su bonificación de pase independiente.

## Recompensa diaria e iconos

1. Abre `/diario` en escala GUI 2, 3 y 4.
2. Michicoins debe usar la moneda dorada con huella rosa.
3. Pokémon aleatorio debe usar el huevo morado con signo de interrogación antes de revelar una especie.
4. Los textos inferiores no deben cortarse ni solaparse.
5. Las tiradas diarias deben entregarse como tickets físicos.

## Pokémon y casino

1. En cara o sello elige un Pokémon: debe mostrarse su modelo real, nombre y nivel; una Poké Ball de respaldo indica fallo.
2. En garra comprueba que el título superior esté debajo de la insignia central.
3. Coloca las nueve mesas y máquinas y revísalas desde frente, laterales y parte trasera.
4. Confirma plintos, molduras, coronas, patas, barandillas y siluetas diferenciadas sin huecos de colisión.

## Regresión

1. Reinicia cliente y servidor y vuelve a probar diario, pase, misiones, trabajos y casino.
2. Revisa los registros `daily-reward-audit.log`, `battle-pass-audit.log` y `economy/transactions.log`.
3. No apruebes la candidata si desaparece un premio, se duplica un reclamo o un retrato vuelve al icono de respaldo.
