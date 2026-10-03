# Cierre de trabajo de fase 7

Fecha: 27 de septiembre de 2026.

Se finaliza aquí el trabajo de fase 7 por solicitud del usuario. Este cierre entrega el estado local alcanzado; no certifica cumplimiento completo de la condición de salida de la especificación ni aprobación para producción. No se interpretó la solicitud como aceptación de pruebas no ejecutadas o eliminación de requisitos.

## Entrega

- Tres proyectos independientes: API Spring Boot con SQL Server, panel Angular y web pública Angular.
- Guía de instalación, operación, respaldo, restauración y traslado: [operations.md](operations.md).
- Trazabilidad PA-01 a PA-24 y cobertura por navegador: [acceptance-matrix.md](acceptance-matrix.md).
- Resultados, comandos y límites de cada recorrido: [phase-seven-evidence.md](phase-seven-evidence.md).
- Ensayo reproducible de recuperación: [verify-local-recovery.sql](../sql/verify-local-recovery.sql).
- Contrato OpenAPI 0.6.0 y migraciones Flyway V1–V7 conservados.

## Verificación alcanzada

Las últimas ejecuciones documentadas suman 68 casos aprobados: 32 Java sobre SQL Server, 31 del panel y cinco de la web pública. No son 68 casos nuevos ejecutados al redactar este cierre. Los builds de ambos frontend terminaron correctamente; el build del panel posterior a la corrección textual de paginación también fue satisfactorio.

Se realizó respaldo COPY_ONLY con CHECKSUM, VERIFYONLY, restauración en una base nueva, CHECKDB y comparación de conteos y hashes de cierres. Se conserva la copia READ_ONLY TourismRecovery_5AEDC910DFBA42DB993E3A8DFB4D9B09 y su respaldo. No se sustituyó la base original.

Chrome y Brave tienen recorridos parciales documentados de acceso, permisos visibles, calendario, persistencia de tareas, consultas históricas y logout. Se verificaron tamaños del calendario en Chrome, vistas de Cobros en Brave, paginación corregida, auditoría y acceso por teclado. Cancelar una tarea vacía funcionó; descartar cambios escritos tiene cobertura automatizada con confirmación simulada, pero no verificación manual completa del diálogo nativo.

## Comprobaciones pendientes, sin aprobación implícita

| Pendiente | Alcance necesario |
| --- | --- |
| PA-19 integral | Todos los formularios, confirmaciones nativas, teclado, foco, zoom y movimiento reducido. |
| RT-09 | Versiones requeridas de Chrome, Edge, Firefox y Safari; Chrome Android y Safari iOS. Brave aporta cobertura adicional. |
| Descargas en navegador | Confirmar archivos PDF/XLSX obtenidos mediante la interfaz y su contenido. Las pruebas del backend de reportes no sustituyen este recorrido. |
| Alta técnica y traslado | Ejecutar el procedimiento interactivo de usuarios y validar traslado a un segundo servidor. El ensayo local no prueba pérdida del equipo. |
| Preparación operativa | Valores reales, usuarios, SMTP, dominio/TLS, permisos, retención y objetivos RPO/RTO; recorrido completo antes de abrir reservas al público real. |

La condición de aceptación total PA-01 a PA-24 de la fase 7 sigue sin acreditarse completamente. La evidencia parcial no permite afirmar ausencia de defectos en las áreas no comprobadas.

## Estado de entrega local

Se conservan los datos ficticios, incluida la caja abierta del 26 de septiembre y la tarea de prueba completada. No se borran registros para cerrar esta etapa. El envío local permanece deshabilitado y no se autoriza producción ni correo a personas reales. Las últimas sesiones de prueba de Chrome y Brave quedaron cerradas.

Este cierre solo actualiza documentación: no modifica código, esquema, credenciales ni configuración. No se inicia otra fase ni se repiten pruebas sin cambios técnicos.
