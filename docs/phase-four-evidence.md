# Evidencia de fase 4: ingresos y cobros

Fecha local de ejecución: 25 de septiembre de 2026, America/Costa_Rica. SQL Server 2025 Developer local; Java 21; Spring Boot 4.1.1; Angular 22.1.7. Desarrollo en los tres repositorios independientes. No se publicaron servicios ni se realizaron pagos o envíos de correo reales.

## Resultado reproducible

- `business-api/scripts/verify.ps1`: BUILD SUCCESS, 21 pruebas, cero fallos, errores u omisiones. Incluye el contrato OpenAPI 0.4.0 y las pruebas de fases anteriores.
- `admin-web`: `npm.cmd test`, ocho pruebas aprobadas; `npm.cmd run build`, compilación de producción aprobada. Se ejecutaron en la copia temporal NTFS con dependencias bloqueadas; el build se copió a `admin-web/dist`.
- Flyway V5 aplicada y validada en TourismPhaseOneTest y TourismLocal. No se modificaron migraciones previamente aplicadas.
- customer-web no recibió cambios en esta fase; sus cinco pruebas y build de fase 3 siguen documentados en la evidencia anterior, sin atribuir una nueva ejecución.

## Matriz de esta entrega

| Prueba | Evidencia y alcance |
| --- | --- |
| PA-06 | AdmissionConcurrencyTest usa dos hilos, conexiones/transacciones independientes y commits reales: dos intentos por el último cupo producen un ingreso; dos solicitudes con la misma clave producen un ingreso/pago y la misma respuesta. Un trigger efímero provoca fallo al insertar Payment después de insertar Admission: ambos se revierten, no se conserva respuesta idempotente y el reintento puede completarse. AdmissionTest rechaza una segunda admisión de la misma reserva con otra clave. |
| PA-07 | Grupo sin contacto obtiene VIS aleatorio y se encuentra por código; efectivo insuficiente no inserta ingreso; 17 000 CRC recibidos con 20 000 generan vuelto 3 000. Listados con búsqueda vacía incluyen todos los grupos de la fecha; se corrigió el comportamiento CHARINDEX vacío de SQL Server y se dejó una regresión. |
| PA-08 | Reserva de dos adultos mantiene 10 000 CRC aunque una tarifa posterior sea 9 000 por adulto. SINPE registra un solo pago, cambia a CHECKED_IN y no duplica asistencia. No se integra ni se consulta un banco. |
| PA-10, correcciones | Incremento de adulto genera revisión y ADDITIONAL_PAYMENT por 5 000, con recibido 6 000 y vuelto 1 000. Reducción de dos adultos crea devolución vinculada de 10 000. Se conserva el Payment original de 17 000. Contacto se edita con versión; versiones obsoletas se rechazan. Corrección histórica valida cupo original y asigna diferencia a la caja actual, sin contar la visita como asistencia de hoy. Corrección gratuita crea revisión sin movimiento financiero. |
| PA-10, devolución comercial | Pendiente de fase 5: la devolución sin cambio de asistencia corresponde a Finanzas/RF-24. Esta entrega no declara aprobada esa parte de PA-10 ni expone todavía el endpoint comercial. |
| PA-23 | Entrada gratuita consume personas y asistencia, sin crear Payment. Requiere caja abierta al ingresar. |
| Permisos y CSRF | STAFF recibe 403 al abrir caja o cotizar/confirmar correcciones; no recibe openingFloat. Mutación sin CSRF recibe 403. El panel oculta correcciones a STAFF. Caja antigua bloquea apertura y cobro de hoy. |
| Edición de reserva | Conserva tarifas originales, revisa cupo y versión, cambia fecha futura y encola RESERVATION_CHANGED en el idioma guardado. La cotización de edición se calcula en servidor. |
| Respuesta incierta | Vitest comprueba cuerpo/clave idénticos al reintentar, bloqueo de nuevas operaciones y conservación ante expiración de sesión, requiriendo el usuario original. También verifica vuelto exacto con valores mayores que Number.MAX_SAFE_INTEGER, Cancelar y los tres idiomas. |

## Recorrido en navegador

Se inició sesión ADMIN en el navegador integrado, se abrió Cobros e ingresos y se comprobó el listado diario, la reserva de demostración y el bloqueo por caja anterior abierta del 23 de septiembre. La vista 390×844 mostró el menú como selector y los grupos debajo del formulario; se restauró el tamaño normal.

Se creó `RES-2287DE23D3174305ABE68DC7A9A47FA2`, nombre ficticio «Prueba local fase cuatro», visita 26 de septiembre, dos adultos, un niño, un adulto mayor y un vehículo: 17 000 CRC. Se editó a tres adultos, con revisión del servidor por 22 000 CRC, y después se canceló con motivo «Fin de prueba local de fase 4». El detalle mostró CANCELLED y conservó historial. SQL confirmó eventos RESERVATION_CONFIRMED, RESERVATION_CHANGED y RESERVATION_CANCELLED en PENDING. No hubo envío SMTP.

No se completó un cobro por navegador sobre TourismLocal porque su caja anterior sigue abierta. No se alteró ese historial para sortear la regla. El flujo de dinero se verificó en SQL Server aislado mediante los servicios transaccionales y los formularios mediante Vitest. El recorrido de cobro/cierre integrado se completará en fase 5.

## Datos y límites

Las pruebas transaccionales usan rollback. AdmissionConcurrencyTest conserva registros financieros inmutables en fechas históricas propias de TourismPhaseOneTest y su actor inactivo `admission.concurrent.*` con contraseña inutilizable; cierra técnicamente solo su caja de fixture y retira su configuración. No crea CashClosure ni constituye evidencia del cierre operativo. El trigger efímero se retira en finally; una interrupción abrupta exige revisar exclusivamente los triggers TR_TestPaymentFailure_* antes de repetir. Los datos ficticios de demostración permanecen separados.

La idempotencia del servidor se conserva al menos 49 horas, sin purga automática. La clave pendiente del panel se conserva en memoria: un cierre o recarga pierde ese estado y exige verificar el grupo antes de repetir. Los datos personales no se guardan en almacenamiento persistente del navegador.

Pendientes: cierre inmutable y su concurrencia con cobros, gastos, devolución comercial, reportes PDF/Excel y envío de cierres (fase 5); calendario/tareas y pantallas de actividad; matriz completa de navegadores; guías y validación operativa final. Esta evidencia no declara el proyecto listo para producción.
