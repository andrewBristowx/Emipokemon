# Guía de pruebas — Emipokemon 0.4.0-alpha.71

Instala exactamente el mismo JAR alpha.71 en servidor y cliente, reemplazando alpha.70.

## 1. Retratos gacha

1. Abre el gacha normal.
2. Ejecuta una tirada x10.
3. Confirma que cada tarjeta muestre el Pokémon y no solo nombre/rareza/nivel.
4. Comprueba que ningún retrato tape el nombre, rareza o nivel.
5. Repite una x1 y verifica que el retrato grande sea visible y centrado.
6. Repite en el gacha especial de Emi.

## 2. Display sobre las máquinas

1. Aléjate entre 5 y 12 bloques de ambas máquinas.
2. Debe verse `POKÉMON DE TEMPORADA` por encima del techo.
3. Debajo debe aparecer el nombre real del destacado.
4. Debe aparecer el modelo 3D del Pokémon entre la máquina y el texto.
5. Sal del área, vuelve a entrar y espera como máximo 2 segundos: el display debe reaparecer gracias al refresco S2C.
6. Reinicia cliente/servidor y repite.

## 3. Máquina de garra

Revisa desde frente, atrás, izquierda y derecha:

- no debe quedar la franja/hueco inferior visible de alpha.70;
- base y gabinete deben verse unidos;
- las cuatro esquinas deben conectar con los marcos superior e inferior;
- no debe abrirse ninguna junta al cambiar el ángulo de cámara.

## 4. Regresión

- Los fondos del gacha deben seguir iguales.
- El rate base Legendary + Mythical debe seguir en 1 % combinado.
- Pity y tickets deben persistir tras reiniciar.
- La máquina de garra debe conservar controles y premios.

Si un punto visual falla, adjunta una captura desde el mismo ángulo y `latest.log` desde el arranque hasta abrir el gacha o mirar la máquina.
