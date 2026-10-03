# Evidencia de fase 1

Fecha: 23 de septiembre de 2026, hora de Costa Rica. Estado: base de fase 1 verificada, con las limitaciones de alcance indicadas al final.

## Ejecución comprobada

`./scripts/verify.ps1`: BUILD SUCCESS. Ocho pruebas ejecutadas, cero fallos, cero errores, cero omitidas. Seis pruebas de DatabaseFoundationTest utilizan SQL Server 2025 real en TourismPhaseOneTest. Dos pruebas de FoundationTest validan contrato OpenAPI y rechazo de perfiles/bases no locales para el seed.

PA-16 inicial: auditoría de inserción, actualización y eliminación de varias filas; ausencia de eventos para actualizaciones sin cambios; detección de cambios de mayúsculas/espacios finales; datos sensibles ausentes del JSON; actor de aplicación y origen EXTERNAL; limpieza de actor y solicitud antes de devolver una conexión reutilizada al pool.

PA-20 inicial: migraciones sobre base vacía realizadas el 21 de septiembre; V3 aplicada y validada el 23. Repetir migrate no vuelve a aplicar migraciones. El esquema contiene 23 tablas de aplicación y no crea usuarios/configuración de demostración automáticamente. Seed separado probado dentro de transacción y cargado posteriormente en TourismLocal mediante la herramienta local.

También comprobados: restricciones de fechas únicas, cupos no negativos y tasas positivas; cierre de muestra con esperado 63000 CRC, diferencia -500 CRC y resultado 83000 CRC; tres métodos de pago; rechazo de modificación del cierre. Estas comprobaciones del esquema y los datos no declaran aprobadas las pruebas funcionales financieras de fases posteriores.

`./scripts/database.ps1 migrate` y `./scripts/database.ps1 seed`: completados en TourismLocal. Migraciones V1, V2 y V3 aplicadas mediante Flyway; salida del seed: Local seed applied.

Permisos del usuario tourism_app_local comprobados mediante EXECUTE AS USER y una transacción revertida: no puede modificar AuditLog, AppUser ni Payment; una inserción en ClosedDate sí genera su fila de auditoría por el trigger.

## Frontend y ejecución local

Ambos proyectos Angular se instalaron sobre NTFS temporal y generaron sus package-lock.json. El disco D es FAT32 y presentó fallos de extracción; no se trasladaron los repositorios ni se modificaron otras bases. Los scripts scripts/run-local.ps1 de cada frontend reproducen el entorno temporal.

Cada frontend aprobó dos pruebas de idioma/persistencia y compiló para producción. Vitest quedó fijado en 4.1.11, con un trabajador y sin almacenamiento experimental de Node. La versión 4.0.8 produjo errores de arranque de trabajadores en instalaciones nuevas; no se conservaron sus resultados como única evidencia.

Se ejecutó además customer-web/scripts/run-local.ps1 test desde una carpeta temporal nueva: npm ci instaló 377 paquetes usando el lockfile y las dos pruebas pasaron. Esto verifica el comando de ejecución desde D sin reutilizar node_modules.

En el navegador integrado se verificaron ambos frontend, selección de español/inglés/portugués, persistencia al recargar y anchuras de 360, 768 y 1440 píxeles sin desbordamiento horizontal. Estas comprobaciones cubren solo las pantallas iniciales y no sustituyen RT-09 completo.

## Pendiente fuera de la aceptación inicial

Arranque HTTP comprobado: GET /api/v1/health devuelve 200 con status UP y phase FOUNDATION; GET /api/v1/reservations sin sesión devuelve 401 con el formato de error definido.

Pendientes: recorrido interactivo de alta/restablecimiento técnico, recorridos de negocio y matriz de navegadores. El seed BCrypt se comprobó sobre SQL Server; la herramienta interactiva separada fue compilada. No se ejecutó ninguna prueba de SMTP, PDF, XLSX, concurrencia de cupo o cobro. Las fases 2–7 siguen sin implementar.

Los informes JUnit reproducibles se generan en target/surefire-reports al ejecutar verify. No se incluye .env ni credenciales de infraestructura en esta evidencia.
