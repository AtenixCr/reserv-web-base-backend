# Evidencia de fase 3: reservas públicas

Fecha: 25 de septiembre de 2026. Entorno local, datos ficticios, SQL Server real y receptor SMTP efímero en loopback. Las fases 4–7 siguen pendientes.

## Comandos y resultados

`business-api/scripts/verify.ps1`: 14 pruebas, cero fallos, cero errores y cero omisiones; BUILD SUCCESS. Incluye las nueve pruebas anteriores, cuatro recorridos integrados de reservas y correo y una prueba de concurrencia con conexiones y commits independientes.

`customer-web`: `npm.cmd test` y `npm.cmd run build`, ejecutados en la copia temporal NTFS. Cinco pruebas aprobadas y build de producción correcto. Se copió dist al repositorio. El panel administrativo no cambió de código en esta fase; conserva las tres pruebas y el build de fase 2, sin contabilizarlos como una ejecución nueva.

Las pruebas Java usan exclusivamente TourismPhaseOneTest, cuya identidad se comprueba antes de ejecutar. Las pruebas ordinarias revierten sus datos. La prueba concurrente confirma operaciones reales y después elimina únicamente sus filas temporales relacionadas; deja la tarifa inmutable y auditoría como evidencia. No se creó otra base ni se ampliaron permisos. No ejecutar dos copias de la suite al mismo tiempo.

## Aceptación comprobada

| Prueba | Evidencia ejecutada |
| --- | --- |
| PA-01 | Hoy y hoy + 365 rechazados; mañana y hoy + 364 aceptados en cotización si hay cupo. Calendario incluye límites y fecha del servidor. |
| PA-02 | Cero personas, valores negativos y contacto inválido rechazados; cantidades fraccionarias rechazadas por JSON; 2 adultos + 1 niño + 1 mayor + 1 vehículo dan 17 000 CRC. |
| PA-03 | Dos hilos sincronizados compiten por un solo cupo en transacciones independientes: una confirmación y un conflicto; SQL conserva una persona. Dos solicitudes simultáneas con la misma clave devuelven la misma reserva y un único evento de correo. |
| PA-04 | No-show anticipado devuelve conflicto; cancelación libera el cupo y una segunda cancelación con versión obsoleta falla. La prueba de configuración de fase 2 sigue rechazando bloqueo y reducción incompatibles. |
| PA-05 | Publicar nuevas tarifas devuelve RATE_CHANGED al confirmar una cotización anterior; una reserva confirmada mantiene el total anterior; repetir su clave recupera el mismo resultado incluso tras cambiar tarifas. |
| PA-15 | Fallo SMTP conserva la reserva y el evento; se verifican 1, 5, 15 y 60 minutos de demora, quinto fallo terminal, recuperación de bloqueo mayor a 10 minutos y rechazo de resultados de intentos antiguos. Receptor SMTP local acepta un mensaje con Message-ID estable. |
| PA-18 | Plantillas es/en/pt, contenido portugués y total CRC comprobados; formulario observado en los tres idiomas; conversiones exactas mantienen sus pruebas previas. |
| PA-24 | Diez confirmaciones con una clave conservan una reserva; siguiente intento devuelve 429 con Retry-After. Público funciona sin sesión, acceso anónimo a reservas internas devuelve 401. CSRF y permisos administrativos siguen pasando. Error de red en frontend reutiliza cuerpo y clave, y conserva datos. |

La aceptación SMTP local no demuestra entrega a un buzón externo. SENT significa aceptación del servidor. No se esperaron físicamente 81 minutos: las pruebas verifican availableAt y adelantan únicamente los trabajos ficticios de la transacción de prueba. Se simuló una antigüedad de 11 minutos para recuperar el arrendamiento; no se apagó el proceso en mitad de una transmisión real.

## Recorrido en navegador

Se habilitó el negocio ficticio mediante la API administrativa y se cerró esa sesión. Desde `http://localhost:4200/` se seleccionó el 26 de septiembre, se ingresó «Prueba local fase tres» con `fase3@example.invalid`, 2 adultos, 1 niño, 1 adulto mayor y 1 vehículo. La revisión mostró 17 000 CRC y la confirmación creó `RES-0AEF5F134E364F1AA5386CC3319962E8`, con horarios, desglose, número SINPE, verificación manual y correo pendiente. Al abrir el calendario nuevamente, el cupo de esa fecha pasó de 100 a 96. Esa reserva ficticia permanece en TourismLocal.

Se observaron las etiquetas del formulario y calendario en español, inglés y portugués. Se comprobó Cancelar con formulario vacío; el descarte con cambios tiene prueba automatizada. Se verificó por DOM ausencia de desbordamiento horizontal con el formulario abierto a 360, 768 y 1440 píxeles (anchos de contenido 345, 753 y 1425 respectivamente). Se inspeccionó visualmente el formulario móvil. Se restauró el tamaño normal y se dejó el sitio en español y CRC.

La matriz completa de navegadores, lectores de pantalla y dispositivos de RT-09 no se ejecutó; corresponde a la verificación final. No se certifica accesibilidad completa por estas comprobaciones.

## Alcance entregado y límites

OpenAPI 0.3.0 fue generado antes de conectar el formulario y validado por swagger-parser. No hacen falta nuevas migraciones ni inserts de instalación: se usan las tablas y permisos de fases anteriores. Se actualizaron README, variables SMTP/límites, diccionario y procedimiento SQL de reintento manual.

Las transiciones cancel/no-show se entregan como API administrativa con permiso, CSRF, motivo y versión. Listado, consulta, alta/edición interna y botones operativos de reservas se incorporarán con las fases 4 y 6. No hay cobros, ingresos ni cierres nuevos en esta fase. La parte de PA-15 relativa a cierres se comprobará en fase 5.

El procesador SMTP permanece deshabilitado en la demostración por defecto. No se creó cuenta externa, no se envió correo a personas reales y no se publicó la aplicación. Falta probar la configuración de un proveedor SMTP autorizado, la entrega real y el reintento manual con credenciales operativas. El aviso de contacto y retención requiere definición del negocio antes de producción.

La idempotencia persiste en SQL; el borrador y la clave del frontend viven en memoria de la pestaña. Ante respuesta incierta se debe usar Recuperar confirmación. Cerrar o recargar esa pestaña pierde su clave local, por lo que debe contactarse al negocio antes de intentar una reserva nueva. Los límites por IP son locales en memoria; reiniciar los reinicia y varias réplicas requieren un contador compartido.
