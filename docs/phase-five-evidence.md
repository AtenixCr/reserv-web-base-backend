# Evidencia de fase 5: Finanzas

Ejecución local: 26 de septiembre de 2026, America/Costa_Rica. Datos ficticios, SQL Server 2025 Developer, Java 21, Spring Boot 4.1.1, Angular 22.1.7. Tres repositorios independientes; no hubo publicación ni movimientos de dinero reales.

## Pruebas reproducibles

- `business-api/scripts/verify.ps1`: suite completa de integración SQL Server, concurrencia, seguridad y contrato OpenAPI. BUILD SUCCESS: 28 pruebas, cero fallos, errores u omisiones. Incluye la regresión UTC y OpenAPI 0.5.0. Log local: `target/phase-five-verify.log`.
- `admin-web`: `npm.cmd test`, once pruebas aprobadas; `npm.cmd run build`, compilación de producción aprobada. Copia temporal NTFS para dependencias, fuentes y distribución conservadas en D.
- PDFBox 3.0.8 y POI 5.5.1. PDF generado y revisado visualmente; tablas/celdas XLSX comprobadas con POI. Archivos de prueba en `business-api/target/phase-five-evidence`.
- Fuente Noto Sans Regular, SIL OFL 1.1; SHA-256 `b85c38ecea8a7cfb39c24e395a4007474fa5a4fc864f6ee33309eb4948d232d5`.
- Flyway V6 validada en la base aislada y aplicada al iniciar TourismLocal; índice único de reversiones. No se modificaron migraciones anteriores ni permisos.
- customer-web sin cambios; sus pruebas de fase 3 no se presentan como ejecutadas de nuevo.

## Matriz de aceptación

| Prueba | Evidencia |
| --- | --- |
| PA-11 | Dos devoluciones simultáneas de 7.000 contra saldo 10.000: una confirma y otra recibe REFUND_EXCEEDS_NET; saldo final 3.000. Cero rechazado; formato negativo rechazado por validación. Asistencia intacta. |
| PA-12 | Fondo 20.000, cobros CASH 50.000/SINPE 30.000/CARD 10.000, gasto CASH 5.000 y devolución CASH 2.000: esperado 63.000, contado 62.500, diferencia −500, resultado 83.000. Nota exigida si hay diferencia. |
| PA-13 | Corrección histórica en jornada siguiente genera adicional 5.000, sin asistencia nueva en esa jornada. PDF extraído y celdas XLSX del cierre anterior permanecen iguales. Cambios de nombre/configuración/contacto tampoco alteran archivos guardados. |
| PA-14 | Instantánea, resumen, PDF y XLSX concilian. Seis hojas fijas, importes exactos en texto y entradas con prefijo = preservadas como texto. Responsable e idioma guardados; zona Costa Rica en presentación. PDF con resumen y secciones paginadas. |
| PA-22 | Cobro y cierre simultáneos: el cobro se incluye completo y la versión obliga a revisar, o el cierre gana y el cobro se rechaza. Nunca queda un pago fuera de la instantánea de una caja cerrada. |
| PA-15 | Fallo de generación y SMTP caído conservan cierre/cola; reintento no duplica evento. Receptor SMTP en loopback acepta PDF y XLSX completos, destinatario/idioma guardados y Message-ID estable. Se verifica SENT como aceptación, no entrega externa. |
| PA-09 | STAFF recibe 403 en gastos, devoluciones, ajustes/cotizaciones, cierre, resumen, saldo, detalle/reporte de cierre y reintento de correo. |
| PA-10 | Devolución comercial mantiene cantidades; compensarla recupera saldo neto sin cambiar Payment original. Reversión y compensación posterior mantienen trazabilidad. |
| Versiones e idempotencia | Clave/cuerpo iguales recuperan la misma respuesta; cierre repetido no duplica instantánea. Contacto y corrección sin diferencia monetaria también invalidan el resumen abierto. El panel conserva clave, cuerpo, usuario y datos ante respuesta incierta/conflicto. |

## Recorrido local

ADMIN cerró la caja ficticia del 23 de septiembre con esperado/contado 21.000, diferencia cero y nota explícita de demostración. Se creó el cierre #2 y un correo pendiente para admin@example.invalid en español. Ambos archivos se descargaron: closure-2.pdf y closure-2.xlsx. El observador de descargas del navegador integrado agotó su espera para el PDF, pero se comprobó el archivo real en Descargas y la operación de exportación no mostró error.

Se abrió la jornada del 26 de septiembre con fondo 20.000. La reserva ficticia RES-0AEF5F134E364F1AA5386CC3319962E8 ingresó con el código VIS-73EF04346E8D4A63956F603A38C5FA90: 17.000 CRC, recibido 20.000, vuelto 3.000. Finanzas reflejó esperado 37.000 y asistencia 2 adultos, 1 niño, 1 adulto mayor, 1 vehículo.

La inspección detectó lectura JDBC de datetime2 UTC interpretada como hora local; se corrigieron lecturas y escrituras explícitas UTC, incluido el repositorio de tasas. Una prueba compara timestamps de respuesta/instantánea con el texto UTC leído directamente de SQL. El cierre ficticio #2 se creó antes de esa corrección: conserva su instantánea original por inmutabilidad; su closedAt guardado en JSON es el instante correcto, pero la columna createdAt y la hora del ajuste histórico en esa instantánea reflejan el defecto previo. Este registro solo pertenece a la demostración; no se alteró ni se usa como evidencia de fechas corregidas.

Se registró gasto CASH de 1.000, devolución comercial CASH de 500 y compensación vinculada del gasto por 1.000. El gasto original permanece y la asistencia continúa en 4 personas. Resultado final: cobros 17.000, gastos 1.000, devoluciones 500, compensaciones 1.000; resultado operativo 16.500 y esperado CASH 36.500. La jornada del 26 permanece OPEN para seguir probando. Las nuevas horas del movimiento se mostraron correctamente en Costa Rica.

Se revisó Finanzas a 360, 768 y 1440 píxeles en el navegador integrado. Se corrigió el ancho mínimo del contenedor principal y la división de códigos largos; el ancho desplazable del documento coincide con el ancho visible. En móvil se muestra el selector de navegación, los campos se apilan y los mensajes siguen legibles. Se restauró el tamaño normal. Esto no reemplaza la matriz completa de navegadores de fase 7.

## Límites y operación

SMTP de TourismLocal permanece deshabilitado. No se enviaron correos a personas reales. Los correos nuevos quedan PENDING y solo el receptor loopback recibió mensajes de la suite aislada.

Las pruebas concurrentes conservan movimientos, cierres, tarifas y auditoría inmutables en TourismPhaseOneTest; sus actores quedan inactivos y sus correos aplazados. El resto usa rollback. No ejecutar suites simultáneas ni en una base operativa. Idempotencia conservada al menos 49 horas; los pendientes del navegador permanecen en memoria, no sobreviven al cierre de pestaña.

El seed antiguo schemaVersion 1 carece de nombres/metadatos completos. Su panel lo identifica y sus reportes no reconstruyen historia desde datos actuales. Los cierres nuevos usan schemaVersion 2.

Quedan fase 6 (calendario, tareas y pantalla de actividad) y fase 7 (matriz completa de navegadores, verificación final y guías operativas). Esta fase no declara el proyecto listo para producción.
