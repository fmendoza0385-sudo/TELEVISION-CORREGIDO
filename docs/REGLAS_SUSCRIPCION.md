# Reglas de suscripción (Documento Maestro v1.2, secciones 6 y 21)

- Zona horaria de negocio: `America/Bogota`.
- Corte: **12:00:00 del día 5** de cada mes. Período de gracia: **ninguno** (pendiente de confirmación expresa).
- Plan → lista: `PLAN-50`→`m3u_plan_50.m3u`, `PLAN-80`→`m3u_plan_80.m3u`, `PLAN-120`→`m3u_plan_120.m3u`.
- Subcuentas: `USER-001`, `USER-001-TV2`, `USER-001-MOVIL` (cada una con su propio ID y dispositivo).

| Condición | Resultado |
|---|---|
| Licencia firmada vigente y dispositivo coincide | `ACTIVA` → televisión |
| Faltan ≤ 3 días para el corte | `POR_VENCER` → televisión + aviso no intrusivo |
| `ahora ≥ valid_until` | `VENCIDA` → bloqueo |
| Código incorrecto / usado / de otro usuario / ciclo anterior | Se mantiene activación, mensaje claro |
| Código válido de otro dispositivo | `DISPOSITIVO_NO_AUTORIZADO`, bloqueo |
| Servidor no disponible | `ERROR_SERVIDOR`: informa y reintenta; **nunca** activa |
| Respuesta alterada o sin firma válida | Rechazada |
| Fecha del equipo modificada | Sin efecto (hora del servidor + reloj monotónico; no retrocede) |
