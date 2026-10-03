# Evidencia de fase 7: verificación y operación

Fecha: 27 de septiembre de 2026. Estado actual: trabajo finalizado aquí por solicitud del usuario, con pendientes de aceptación documentados en el [informe de cierre](phase-seven-closeout.md). No hubo publicación ni correo externo. Las secciones siguientes conservan el historial de ejecución y los estados comunicados antes del cierre de trabajo.

## Regresión ejecutada

- `scripts/verify.ps1`: 32 pruebas Java contra SQL Server, cero fallos, errores y omisiones; BUILD SUCCESS. Duración Maven 1:22. Log local `target/phase-seven-verify.log`.
- admin-web: 16 pruebas Vitest aprobadas y build de producción correcto; 438,20 kB iniciales.
- customer-web: cinco pruebas Vitest aprobadas y build de producción correcto; 304,90 kB iniciales.
- Frontend ejecutado en las copias NTFS `tourism-phase-two-20260925/admin-web` y `customer-web`. Se compararon los hashes de src con los repositorios para comprobar que se probaron los mismos archivos.
- Se mantuvieron versiones, contrato OpenAPI 0.6.0 y migraciones V1–V7. No se modificaron reglas de negocio ni esquema en esta fase. FoundationTest validó el contrato durante la regresión.

## Recuperación ejecutada

Comando: `sqlcmd -S lpc:localhost -E -C -b -i sql/verify-local-recovery.sql -o target/phase-seven-recovery.log`. Código de salida 0.

BACKUP COPY_ONLY con CHECKSUM: 1290 páginas; VERIFYONLY válido. RESTORE con MOVE hacia archivos nuevos, sin REPLACE ni DROP, completado. DBCC CHECKDB no reportó errores. Coincidieron conteos de todas las tablas y hashes SHA-256 de las instantáneas de cierres; siete migraciones y ningún trigger deshabilitado.

Copia conservada: `TourismRecovery_5AEDC910DFBA42DB993E3A8DFB4D9B09`, READ_ONLY. Contiene siete reservas, cuatro ingresos, dos cierres, 84 filas de auditoría y 29 triggers. El respaldo con el mismo nombre y extensión .bak permanece en el directorio predeterminado de respaldos del servicio SQL; la ruta exacta está en el log local. La base original no se sustituyó ni se borró. Se ejecutó sin escrituras de esta sesión durante la comparación. La API anterior seguía activa; los conteos comparados no detectaron cambios concurrentes. No se inició worker sobre la copia.

La API existente devolvió status UP y phase ORGANIZATION_AUDIT; el intento de iniciar otra instancia terminó porque 8080 ya estaba ocupado. Se conservó la instancia existente, sin terminar procesos ajenos. Ambos frontend responden en sus puertos locales.

## Navegador y alcance

El inventario de superficies disponibles mostró únicamente el navegador integrado. Se abrió la web pública, cargó el negocio de demostración y se accedió al formulario por teclado. Se introdujo un nombre ficticio y se intentó Cancelar. El control del navegador agotó su tiempo al tramitar el diálogo; no fue posible observar su aceptación/rechazo. Ese paso queda no verificado manualmente. No se confirmó ninguna reserva nueva. La prueba automatizada de Cancelar del frontend público sí pasó en esta ejecución.

Se mantienen como evidencia previa las revisiones responsive y recorridos de las fases 2–6. No se presentan como una nueva prueba en Chrome, Edge, Firefox o Safari ni como validación de todos los formularios en cada navegador.

## Entregables y pendientes

`operations.md`: instalación segura, usuarios/permisos, SMTP, rutina diaria, respaldo, ensayo local y traslado con retorno controlado. `acceptance-matrix.md`: trazabilidad PA-01–PA-24, matriz de navegadores y condición de salida explícita. `sql/verify-local-recovery.sql`: ensayo local reproducible, sin sustituir bases existentes.

Faltan PA-19 manual integral, navegadores requeridos, alta técnica interactiva y traslado a un segundo servidor. También faltan valores reales, aprobación de retención y objetivos RPO/RTO, TLS/proxy/dominio y correo real bajo autorización. No se certifica producción ni recuperación ante pérdida del equipo. La salida de fase 7 permanece abierta.

## Ampliación PA-19 del 27 de septiembre

Se añadieron 15 pruebas del DOM renderizado de Angular, ejecutando el botón Cancelar real mediante click y eventos de entrada. No se sustituyó el comportamiento del componente: únicamente se simuló la respuesta del diálogo nativo y la API. Se comprueba que Cancelar sea un botón no submit, que rechazar conserve el formulario, que aceptar descarte/cierre o restaure valores y que no se realicen solicitudes adicionales.

Cobros: walk-in, reserva nueva, edición de reserva, contacto, corrección de cantidades, cancelación de reserva, no presentado e ingreso de reserva. Finanzas: gasto, devolución, compensación y cierre. Configuración: negocio, tarifas y conversiones. Las pruebas previas conservan cobertura de login, tareas y reserva pública.

Comando ejecutado en la copia NTFS actualizada: `npm.cmd test`. Resultado final: cinco archivos, 31 pruebas aprobadas, cero fallos. Se hizo una ejecución intermedia de 28 y otra final de 31 al incorporar configuración; no se suman entre sí. Solo cambiaron pruebas y documentación: no se modificó el código de producción, contrato ni base de datos en esta continuación. La regresión anterior conserva 32 pruebas Java y cinco públicas aprobadas, para 68 casos automatizados en conjunto; no se reejecutaron esas dos suites sin cambios.

Esto amplía la cobertura automatizada de PA-19; no sustituye pruebas de teclado, movimiento reducido, zoom, diálogos nativos y tamaños en Chrome/Edge/Firefox/Safari. El inventario de esta continuación volvió a mostrar solo el navegador integrado. No se repitió el recorrido del diálogo que había bloqueado el control del navegador.

## Recorrido conectado de Chrome y Brave

El 27 de septiembre se detectaron las extensiones de Chrome y Brave y se abrió localhost:4201 en ambos. No se obtuvo número exacto de versión: no se afirma certificación de una versión concreta.

Chrome: acceso con admin.local, menú completo y calendario del mes actual. Se abrió Nueva tarea y se introdujo un borrador ficticio. Al pulsar Cancelar la herramienta detectó un diálogo confirm, pero agotó el tiempo al intentar cerrarlo. Se solicitó intervención del usuario; no se confirmó desde la herramienta que se conservara o descartara el borrador. No se guardó esa tarea en Chrome.

Brave: acceso con staff.local, únicamente Inicio/Cobros/Calendario visibles. La caja ficticia del 26 impidió nuevos ingresos el 27. La búsqueda histórica del 26 mostró reservas cancelada e ingresada y su admisión. Se creó y verificó «Prueba Brave fase 7», pendiente para el 27, autor y último editor Demo Staff. Se comprobó cambio a inglés. En las vistas de 360 y 768 píxeles coincidieron clientWidth/scrollWidth (345 y 753); se inspeccionó visualmente Cobros en móvil. No se completaron 1440, todos los idiomas, diálogos, cierre/logout ni el recorrido financiero completo en estos navegadores. Brave es cobertura adicional, no sustituto de Edge/Firefox/Safari.

Se detectaron flechas duplicadas en los botones de paginación de Cobros: la traducción ya incluía la flecha y el template agregaba otra. Se eliminaron únicamente las flechas literales de operations.html. El primer intento de compilar quedó sin ejecutar por límite de uso del revisor automático. En la continuación se reintentó con revisión y `npm.cmd run build` terminó correctamente: 438,18 kB iniciales, artefacto copiado a admin-web/dist. Las 31 pruebas del panel son las de la ejecución anterior; no se reejecutaron para este cambio textual. La corrección aún no se ha comprobado visualmente en los navegadores conectados.

Al reanudar, el inventario mostró solo el navegador integrado y ninguna pestaña: Chrome y Brave no estaban disponibles. Se pidió abrirlos con sus extensiones activas y volver a mencionarlos. La verificación manual continúa pendiente; no se presenta la desconexión como un defecto de la aplicación.

## Continuación conectada del 27 de septiembre, noche

Se levantaron nuevamente la API local y el panel en 4201, con notificaciones desactivadas. Chrome y Brave permitieron acceso ADMIN. En Chrome se comprobaron las etiquetas corregidas de paginación: «← Anterior» y «Siguiente →», sin flechas duplicadas.

La tarea «Prueba Brave fase 7» se cambió a Completada desde Chrome. Al abrir nuevas pestañas en ambos navegadores, Inicio mostró el estado persistido. El detalle del calendario en Chrome conservó autor Demo Staff y último editor Demo Administrator, 27 sept 2026 a las 21:01. No se creó otra tarea.

En Chrome, el calendario no presentó desbordamiento horizontal de documento a 360, 768 y 1440 píxeles: clientWidth y scrollWidth coincidieron en 345, 753 y 1425 respectivamente. Se inspeccionó captura móvil; esto no certifica todos los componentes ni sustituye zoom, teclado o movimiento reducido. Se restableció el viewport. La captura `phase-seven-chrome-task.png` muestra el calendario de esta ejecución, no el detalle inferior de la tarea.

Brave mostró el resumen financiero del 26, con efectivo esperado 36500 CRC y resultado operativo 16500 CRC, y el bloqueo de movimientos por caja de fecha anterior. La consulta del cierre histórico del 23 mostró sus botones PDF y Excel. La espera de descarga PDF agotó el tiempo de la herramienta: la descarga queda sin verificar, no se registra como aprobada ni como defecto confirmado de la aplicación.

En Brave se comprobó Inicio en portugués, incluida la tarea «Concluída», y se volvió a español. En ambos navegadores se cerró sesión y se recargó: permaneció visible Iniciar sesión, sin panel autenticado. Las pestañas anteriores perdieron el vínculo de automatización; nuevas pestañas recuperaron el acceso sin modificar datos adicionales.

Esta continuación no cambió código ni repitió suites automatizadas. Permanecen pendientes los diálogos nativos, descargas verificadas y el resto de la matriz integral. La fase 7 sigue abierta.

## Auditoría y teclado, continuación de las 21:07

Chrome: se consultó el cierre #2 del 23 de septiembre. La interfaz mostró efectivo contado de 21000 CRC, diferencia cero, reporte guardado en español y correo Pendiente con cero intentos. Al pulsar PDF, Actividad registró «Reporte exportado» a las 21:07:25; también figuraba la solicitud anterior de las 21:01:46. Esto acredita la solicitud de exportación, no la integridad ni la presencia del archivo descargado. El acceso a chrome://downloads fue bloqueado por la política del navegador; no se intentó eludir el bloqueo.

En Actividad se filtró por Entidad Tarea y Registro modificado: dos resultados. Se abrió el del 27 a las 21:01:44 y mostró Tarea #4, Demo Administrator y Resultado Modificado · Aplicación. El detalle no expuso el título ni la descripción privada. No se afirma inspección de valores antes/después, que no aparecen en esta interfaz.

Brave: acceso STAFF usando Tab desde Usuario (foco confirmado en contraseña) y Enter para enviar. Se abrió Nueva tarea vacía; Guardar cambios estaba deshabilitado. Cancelar cerró el formulario y permaneció Tareas (1), sin crear otra tarea. Este caso no cubre la confirmación nativa al descartar cambios escritos. Ambos navegadores finalizaron mostrando Iniciar sesión después del logout.

No se modificaron movimientos financieros, código ni esquema; no se repitieron suites automatizadas. Las descargas y PA-19 integral permanecen pendientes.
