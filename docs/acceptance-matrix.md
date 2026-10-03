# Matriz de aceptación consolidada

Corte: 27 de septiembre de 2026. Las suites agrupan varias comprobaciones por método; esta tabla no equivale a 24 pruebas independientes ni certifica todos los navegadores. Evidencia histórica en phase-one a phase-six; ejecución consolidada en phase-seven-evidence.md.

| PA | Evidencia ejecutable | Alcance |
| --- | --- | --- |
| PA-01 | PublicReservationTest | Límites de fecha |
| PA-02 | PublicReservationTest | Cantidades y 17000 CRC |
| PA-03 | ReservationConcurrencyTest | Último cupo concurrente |
| PA-04 | PublicReservationTest + AccessConfigurationTest | Bloqueos y transiciones |
| PA-05 | PublicReservationTest + AdmissionTest | Cambio e historia de tarifa |
| PA-06 | AdmissionConcurrencyTest + AdmissionTest | Reintento y rollback de pago |
| PA-07 | AdmissionTest | Código y efectivo |
| PA-08 | AdmissionTest | Ingreso y tarifa histórica SINPE |
| PA-09 | AccessConfigurationTest + AdmissionTest | Roles, CSRF, logout y sesión |
| PA-10 | AdmissionTest + FinanceTest | Corrección y devolución |
| PA-11 | FinanceConcurrencyTest + FinanceTest | Saldo y devoluciones simultáneas |
| PA-12 | FinanceTest | 63000 / -500 / 83000 CRC |
| PA-13 | FinanceTest + AdmissionTest | Cierre histórico inmutable |
| PA-14 | FinanceTest | PDF, XLSX e instantánea |
| PA-15 | PublicReservationTest + FinanceTest | SMTP local, reintento y adjuntos |
| PA-16 | DatabaseFoundationTest + OrganizationTest | DML múltiple y contexto pool |
| PA-17 | OrganizationTest + AccessConfigurationTest | Actividad y privacidad |
| PA-18 | AccessConfigurationTest + frontend + pruebas de correo | Idiomas y tasas |
| PA-19 | Frontend (15 casos adicionales de Cancelar en DOM) + evidencia de fases 2–6 | Cancelar ampliado; falta revisión manual y matriz completa |
| PA-20 | FoundationTest + DatabaseFoundationTest | Seed separado y restricciones |
| PA-21 | OrganizationTest + organization.spec.ts | Calendario y tareas |
| PA-22 | FinanceConcurrencyTest | Cobro frente a cierre |
| PA-23 | AdmissionTest | Ingreso gratuito |
| PA-24 | AccessConfigurationTest + PublicReservationTest + frontend | CSRF, límite y recuperación |

## Matriz de navegadores

| Navegador | Estado |
| --- | --- |
| Integrado de Codex | Recorridos locales documentados en fases 2–6; no sustituye las marcas requeridas |
| Chrome | Parcial: ADMIN, calendario, edición persistida de tarea compartida, paginación corregida y logout tras recarga; calendario 360/768/1440 sin desbordamiento; diálogo Cancelar pendiente |
| Brave | Adicional, parcial: STAFF/ADMIN, permisos, bloqueo de caja anterior, búsqueda histórica, tarea compartida, inglés y portugués en Inicio, consulta financiera y logout; 360/768 sin desbordamiento; descarga PDF sin verificar |
| Edge | Pendiente de ejecución identificada |
| Firefox | Pendiente; no disponible como superficie conectada |
| Safari | Pendiente; no disponible en este entorno Windows |

En cada navegador registrar versión, sistema, fecha y resultado: reserva/cotización/confirmación y Cancelar; acceso/expiración/permisos; configuración/tarifas; ingreso CASH/SINPE/CARD/gratuito y reintento; corrección/gasto/devolución/compensación/cierre/descargas; calendario/tareas/actividad. Probar 360, 768 y 1440 píxeles, teclado sin ratón, foco y avisos de error, zoom, movimiento reducido y Cancelar en cada formulario. Ejecutar con datos ficticios en una base de pruebas, sin envío externo.

## Condición de salida

El trabajo de fase 7 se finaliza por solicitud del usuario; ver [informe de cierre](phase-seven-closeout.md). Las reglas centrales tienen pruebas aprobadas, pero PA-19 integral y la matriz de navegadores permanecen pendientes. La condición de salida técnica de fase 7 no se considera completamente acreditada. No se declara la aplicación lista para producción ni se da por aceptado lo no comprobado.
