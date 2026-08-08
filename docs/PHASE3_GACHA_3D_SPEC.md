# Emipokemon — Fase 3: Gacha 3D

**Estado:** especificación aprobada / lista para implementación  
**Build objetivo:** `0.3.0-alpha.1`  
**Baseline:** Minecraft 1.21.1 · Fabric · Cobblemon 1.7.3 · GeckoLib 4.9.2 · Java 21

## Objetivo

Implementar la primera máquina Gacha 3D física de Emipokemon, conectada al backend de Fase 2. El servidor seguirá siendo la única autoridad sobre coste, RNG, banner, pity, entrega y transacciones; GeckoLib se usará solo para render, animaciones y sincronización visual.

## Alcance inicial

- Máquina Gacha estándar 3D colocable.
- Cámara con cápsulas decorativas, ranura de ticket, botón, salida y cristal superior.
- Ticket estándar y ticket especial Emi.
- `banner_id` configurable por máquina.
- Consumo del ticket requerido por el banner mediante el backend `ITEM`.
- Animaciones `idle`, `activate`, `roll`, `result_common`, `result_epic`, `result_legendary`.
- Sonidos y partículas de primera versión.
- Bloqueo contra doble activación.
- Prueba física con `rayquaza_hoenn`.

Fuera de esta alpha: skin completa de máquina Emi, panel admin visual, Hub/Casino, pantallas 3D complejas y animaciones únicas por Pokémon.

## IDs

| Recurso | ID |
|---|---|
| Máquina estándar | `emipokemon:standard_gacha_machine` |
| Parte superior técnica | `emipokemon:gacha_machine_top` |
| Ticket estándar | `emipokemon:gacha_ticket` |
| Ticket Emi | `emipokemon:emi_special_banner_ticket` |
| Block Entity | `emipokemon:standard_gacha_machine` |

## Dimensiones

- Huella: `1x1` bloque.
- Altura reservada: `2` bloques.
- Altura visual: aprox. `1.65–1.80` bloques.
- La base es el bloque maestro.
- Al colocarla se comprueba aire arriba y se coloca `gacha_machine_top`.
- Romper cualquiera de las dos partes elimina ambas.
- Rotación horizontal con `FACING`.

## Dirección visual

Paleta estándar: blanco crema/cuarzo, rojo rubí, grafito/negro, vidrio azul claro, dorado suave opcional y acentos luminosos rojos.

Piezas obligatorias: base oscura, cuerpo blanco/rojo, cámara de vidrio, cápsulas multicolor, `TICKET IN`, botón rojo, `PRIZE OUT`, cristal superior, emblema frontal y líneas luminosas laterales.

## Jerarquía GeckoLib

Modelo: `standard_gacha_machine.geo.json`

```text
root
  base
  body
  glass_chamber
  capsule_container
    capsule_01 ... capsule_08
  ticket_slot
  main_button
  prize_door
  top_crystal
  emblem
  light_left
  light_right
```

## Texturas

- Máquina: base `64x64`; subir a `128x128` solo si 64x64 pierde legibilidad real.
- Ticket estándar: `64x64`, crema/rojo/dorado, gato de Emipokemon como branding secundario.
- Ticket Emi: `64x64`, lila/rosa/magenta/negro/crema/dorado, Emi chibi como identidad principal y gato como sello secundario.

Los tickets serán modelos finos 3D con frente y reverso. No necesitan GeckoLib mientras no estén animados.

## Tickets

### `emipokemon:gacha_ticket`

Para banners normales o destacados configurados con ticket estándar.

```json
"currency": {
  "type": "ITEM",
  "itemId": "emipokemon:gacha_ticket",
  "amount": 1
}
```

### `emipokemon:emi_special_banner_ticket`

Para banners especiales de Emi o eventos que lo exijan expresamente. No sustituye al ticket estándar.

## Estados

`IDLE -> ACTIVATING -> ROLLING -> REVEAL_* -> IDLE`, con `ERROR -> IDLE` para recuperación.

Mientras no esté en `IDLE`, una segunda interacción debe rechazarse.

## Animaciones

| Animación | Duración objetivo | Comportamiento |
|---|---:|---|
| `idle` | loop | cápsulas suaves, cristal y luces |
| `activate` | 0.4–0.7 s | entrada de ticket y encendido |
| `roll` | 2.5–3.5 s | cápsulas agitadas/girando y luces |
| `result_common` | 0.8–1.2 s | destello breve y salida |
| `result_epic` | 1.2–1.8 s | pausa, luces fuertes y partículas |
| `result_legendary` | 1.8–2.5 s | cristal intenso, vibración y reveal especial |

`MYTHICAL` reutiliza `result_legendary` en la primera alpha.

## Flujo server-authoritative

1. Jugador interactúa.
2. Servidor confirma `IDLE`, banner activo y coste.
3. Servidor crea transacción y decide resultado.
4. Se retira/reserva el ticket.
5. Servidor sincroniza `activate` y `roll`.
6. Al final del reveal, servidor entrega el Pokémon.
7. Entrega confirmada -> actualiza pity y confirma transacción.
8. Fallo -> reembolsa coste y usa `ERROR` brevemente.
9. Regresa a `IDLE`.

## Transacción segura

Campos: `transactionId`, `playerUuid`, `machinePos`, `bannerId`, coste, resultado y estado.

Estados: `PREPARED`, `ANIMATING`, `COMMITTED`, `ROLLED_BACK`.

Reglas:
- un jugador no puede tener dos tiradas activas;
- una máquina no atiende dos jugadores simultáneamente;
- pity solo cambia tras entrega confirmada;
- si hay reinicio con transacción incompleta, debe resolverse/reembolsarse antes de permitir otra;
- el cliente nunca decide ni propone el resultado.

## Block Entity

```text
bannerId: String
machineState: GachaMachineState
activeTransactionId: UUID?
activePlayerUuid: UUID?
resultTier: GachaTier?
animationStartTick: long
```

## Networking

Payloads iniciales:
- `GachaMachineStateS2C`
- `GachaRevealS2C`

El servidor transmite posición, estado, tier de reveal y timestamps. El Pokémon exacto puede notificarse al final tras la entrega.

## Comandos admin propuestos

```text
/emipokemon gacha machine setbanner <banner>
/emipokemon gacha machine info
/emipokemon gacha machine reset
/emipokemon give ticket standard [jugador] [cantidad]
/emipokemon give ticket emi [jugador] [cantidad]
```

## Sonidos reservados

- `emipokemon:gacha_insert`
- `emipokemon:gacha_roll`
- `emipokemon:gacha_reveal`
- `emipokemon:gacha_reveal_epic`
- `emipokemon:gacha_reveal_legendary`

La alpha puede usar placeholders de Minecraft mientras se producen assets definitivos.

## Compatibilidad

Desde Fase 3, Emipokemon debe estar instalado en servidor y clientes por los modelos/animaciones personalizados.

## Criterios de aceptación de `0.3.0-alpha.1`

- [ ] Colocación y rotación correctas.
- [ ] Huella 1x1 y reserva vertical de 2 bloques.
- [ ] Modelo fiel a la referencia aprobada.
- [ ] Animaciones principales funcionando.
- [ ] Tickets correctos en inventario, mano, suelo e Item Display.
- [ ] Consume exactamente el ticket configurado.
- [ ] Sin ticket no inicia.
- [ ] Una interacción = una transacción.
- [ ] Sin doble uso simultáneo.
- [ ] RNG solo en servidor.
- [ ] Premio correcto al equipo o PC.
- [ ] Pity solo tras entrega confirmada.
- [ ] Fallo devuelve coste.
- [ ] Reinicio no duplica ni elimina recursos.
- [ ] `rayquaza_hoenn` funciona desde la máquina física.

## Orden de implementación

1. Registrar tickets y cerrar prueba `ITEM` pendiente de Fase 2.
2. Registrar bloque, bloque superior y Block Entity.
3. Crear modelo/textura y renderer GeckoLib con `idle`.
4. Implementar colocación `1x1x2` y `FACING`.
5. Crear tickets finales.
6. Conectar clic derecho al banner.
7. Añadir transacción y locks.
8. Implementar `activate`, `roll` y reveals.
9. Añadir sonido, partículas y comandos admin.
10. CI + prueba real en Cobbleverse antes de validar.

## Decisiones visuales cerradas

- Primera máquina: estándar rojo/blanco/negro.
- Gatito: branding secundario del ticket estándar.
- Emi chibi: identidad principal del ticket especial.
- Ticket Emi: lila/rosa/magenta/negro con crema/dorado.
- Máquina Emi completa: variante futura.
- Solo se alterará el concepto aprobado si una limitación real de Minecraft/GeckoLib lo exige.
