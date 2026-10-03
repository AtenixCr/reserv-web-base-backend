# Evidencia de fase 2: acceso y configuración

Fecha: 25 de septiembre de 2026. Entorno local de desarrollo, datos ficticios y SQL Server 2025 Developer. No representa autorización de operación o despliegue.

## Resultado automatizado

`./scripts/verify.ps1`: BUILD SUCCESS; nueve pruebas Java, cero fallos, cero errores y cero omisiones. Se usó TourismPhaseOneTest existente, sin crear otra base ni ampliar sus privilegios. Los cambios de prueba se revierten transaccionalmente. Hibernate validó el mapeo JPA de AppUser y Flyway validó las cuatro migraciones.

Las seis pruebas SQL de la base y las dos de contrato/seed siguen pasando. La prueba integrada AccessConfigurationTest recorre múltiples solicitudes MockMvc con filtros reales de Spring Security y SQL Server real:

- Login BCrypt, rotación del identificador, identidad y logout con invalidación.
- Anónimo 401; STAFF 403 al configurar o cerrar; inicio STAFF permitido.
- CSRF ausente/inválido 403; cinco fallos de login y siguiente intento 429 con Retry-After; actividad de fallos sin contraseña.
- Tiempo de inactividad configurado en 1800 segundos y rechazo de sesión con más de 12 horas. No se esperaron físicamente 30 minutos.
- Creación/edición de configuración, versión obsoleta 409, validación de teléfono y horarios solapados.
- Reducción de cupo y bloqueo de fechas ocupadas rechazados con conflicto.
- Tarifas nuevas sin modificar las anteriores; valores decimales como cadenas; tasas y proyección pública.
- Cambio de horario crea eventos persistentes con versión; reintentar una versión obsoleta no duplica eventos.

En las copias NTFS de ambos frontend se ejecutaron `npm.cmd test` y `npm.cmd run build`. Resultado: tres pruebas de admin-web y dos de customer-web aprobadas; ambos builds de producción aprobados. Los artefactos se copiaron a los respectivos directorios dist.

Admin: conservación de credenciales ante error, Cancelar con confirmación, ocultación de configuración para STAFF, traducción y conservación del borrador ante conflicto. Público: fallback de traducción, monedas deshabilitadas sin tasa, conversión sin alterar CRC, redondeo HALF_UP y cantidades grandes sin pérdida de precisión.

## Verificación en navegador integrado

Se comprobó login ADMIN con el usuario ficticio, bienvenida, datos reales del inicio y carga del formulario de configuración con traducciones, horarios y cupos. Se comprobó que Cerrar sesión regresa al formulario de acceso. Se inició sesión STAFF y se confirmó que configuración, finanzas y actividad no aparecen. Se cerraron ambas sesiones de prueba y se dejó la página pública en español y CRC.

La página pública cargó la información guardada en SQL Server. Se observaron español, inglés y portugués. Al seleccionar USD se mostraron adultos a US$ 10,00 a partir de CRC 5000,00, fecha de tasa y aviso de estimación con pago en CRC. La vista móvil a 360 píxeles presentó encabezado, selectores, descripción y tarjetas legibles.

La herramienta del navegador quedó bloqueada al interactuar con el diálogo nativo de confirmación de Cancelar. Se continuó en una pestaña nueva; no se considera aprobada esa comprobación manual. Su lógica sí tiene cobertura automatizada. La captura de escritorio quedó recortada por el panel, por lo que no se usa como evidencia de revisión completa a 1440 píxeles.

## Correspondencia y límites

PA-09: permisos de configuración/cierre y logout cubiertos; los servicios de reservas y cierre se implementan en fases posteriores y permanecen denegados.

PA-18: idiomas y conversiones cubiertos para estas pantallas. La cola de cambios de horario guarda el idioma, pero la entrega del correo en ese idioma no está comprobada: todavía no hay worker SMTP.

PA-24: CSRF, límite de login, errores seguros y conservación de formularios cubiertos. El límite de reservas públicas corresponde a fase 3, junto con su endpoint.

No se declara aprobada la matriz completa PA-01 a PA-24. Faltan pruebas simultáneas reales de reservas, ingresos y cobros; cola con reintentos SMTP; matriz de navegadores y accesibilidad completa; reportes y guías operativas. No se enviaron correos ni se publicaron servicios. El procedimiento interactivo de alta/restablecimiento de usuarios sigue pendiente de ensayo.

## Contrato y continuación

`docs/openapi.yaml` versión 0.2.0 enumera los endpoints disponibles; swagger-parser validó su estructura y se comprobaron los patrones de decimales con importes fraccionarios. La migración V4 permite desactivar tasas en el entorno local sin ampliar otros permisos. No hay inserts adicionales ni seed automático.

La fase 3 debe incorporar reservas públicas, disponibilidad concurrente y procesamiento de la cola, manteniendo el orden de bloqueo BusinessSettings antes de VisitDay por fecha. La web devuelve reservas deshabilitadas hasta que esas garantías estén implementadas y verificadas.
