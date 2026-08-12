# Guía de pruebas — Emipokemon 0.4.0-alpha.67

1. Reemplaza únicamente el JAR anterior y conserva la configuración/mundos.
2. Abre el gacha normal y el de Emi; confirma que sus fondos grandes siguen iguales.
3. Ejecuta x1 y x10; no debe aparecer video y cada resultado debe mostrar el Pokémon, nombre, tier y nivel.
4. Coloca nuevamente todas las máquinas/mesas para comprobar sus texturas restauradas.
5. Mira la máquina de garra desde frente, lados y arriba: deben verse el interior, la garra y los rieles conectados.
6. Abre la mesa de Pokémon y confirma que retrato/nombre/nivel/estado caben dentro de la tarjeta.
7. Abre dados con textos largos y confirma que no cruzan los bordes del panel derecho.
8. Prueba `/kits` y `/kit <id>` con jugadores de cada grupo real.
9. Revisa trabajos: `default`/`michiba` 2, `michivip`/`michidonador` 4, `michimod`/`michidueñas` todos.
10. En Admin → Kits usa un nodo o `groups:michivip,michidonador` y vuelve a probar.

Si un Pokémon no aparece, conserva `logs/latest.log`; el renderizador registra sólo el primer error de Cobblemon para evitar spam.
