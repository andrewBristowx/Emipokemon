# Emipokemon 0.4.0-alpha.64

## Recompensa diaria

- Añade `/diario` y apertura automática al entrar cuando existe una recompensa disponible.
- Usa el día de calendario de `America/Lima`, no un temporizador vulnerable a reconexiones.
- Incluye una animación gacha y un pool ponderado configurable con Michicoins, diamantes, objetos de Cobblemon, tiradas estándar, tickets de garra, tiradas Emi y Pokémon reales.
- Guarda primero la reserva y el progreso diario; después entrega premios externos y deja auditoría/recuperación para reinicios.
- Entrega objetos al inventario o los deja de forma segura junto al jugador cuando no hay espacio.

## Pase infinito

- Añade `/pase`, `/battlepass` y la tecla `P` con páginas ilimitadas de ocho niveles.
- La fila superior es gratuita: una tirada Emi cada cuatro niveles.
- La fila inferior es premium para VIP, Donador, moderación, Emi, operadores o el permiso configurable: diez tiradas Emi en el nivel 1 y dos cada cuatro niveles posteriores.
- El acceso premium es retroactivo: al obtener rango se pueden reclamar niveles alcanzados anteriormente.
- Otorga experiencia por tiempo activo, misiones, capturas, especies nuevas, evoluciones, combates, exploración y niveles de trabajo.
- Limita los eventos de captura por minuto y usa una curva de experiencia ilimitada con coste máximo configurable.

## Gacha y casino

- Añade una cartera persistente para tiradas virtuales estándar y Emi; las máquinas consumen primero el crédito virtual y conservan el ticket físico como alternativa.
- Corrige el retrato real de Cobblemon 1.7.3 en cara o sello al admitir su firma de render de 13 parámetros, manteniendo compatibilidad con la firma antigua de 16.
- Retira el saldo de Michicoins de las interfaces de garra y cara o sello Pokémon.
- Amplía el gabinete de garra con marquesina, vitrina, controles y cajón, y diferencia la mesa baja de cara o sello con dos puestos y moneda central.

## Datos y configuración

- Migra los datos de jugador a la versión 6 sin reiniciar saldos, pity, misiones, trabajos ni progreso previo.
- Migra la configuración a la versión 6 e incorpora secciones editables `dailyReward` y `battlePass`.
- Añade fondos gráficos originales para diario y pase, diseñados sin texto incrustado para conservar traducción y escalado.

Esta compilación debe validarse todavía dentro del modpack real con Java 21, cliente y servidor antes de marcarla como estable.
