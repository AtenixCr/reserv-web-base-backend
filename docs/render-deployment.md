# Publicar la API en Render y conectar los frontends

Render ejecuta la API en un servidor de Internet, construye su imagen desde GitHub y le proporciona HTTPS. La base de datos sigue en tu SQL Server y las interfaces siguen en mediación. Docker describe cómo instalar Java, compilar y arrancar la API; no necesitas instalar Docker para desplegar desde el panel de Render.

## 1. Preparar los accesos

Necesitas acceso a GitHub, una cuenta en https://render.com y permisos para crear un registro DNS dentro de atenix.net. También necesitas la conexión y las credenciales privadas de tu SQL Server. No pegues contraseñas en GitHub ni en los archivos de los frontend.

El backend debe estar actualizado en la rama main de AtenixCr/reserv-web-base-backend, con Dockerfile y .dockerignore. El contenedor compila con Java 21 y Maven Wrapper; no carga .env.remote ni ejecuta semillas. Al arrancar, Flyway valida el historial y aplica las migraciones pendientes: comprueba que la conexión corresponde a tu base y conserva un respaldo antes del primer despliegue.

## 2. Crear el servicio

1. Entra a https://dashboard.render.com y accede con GitHub.
2. Autoriza a Render a leer el repositorio AtenixCr/reserv-web-base-backend. Si no aparece, revisa los permisos de la organización AtenixCr y de la aplicación GitHub de Render.
3. Selecciona New > Web Service y conecta ese repositorio.
4. Completa los campos:

| Campo | Valor |
| --- | --- |
| Name | reserva-api (o un nombre disponible) |
| Branch | main |
| Language / Runtime | Docker |
| Root Directory | Vacío: el backend ya es un repositorio independiente |
| Dockerfile Path | ./Dockerfile |
| Docker Build Context | . |
| Docker Command | Vacío: se usa el comando del Dockerfile |
| Health Check Path | /api/v1/health |

5. Selecciona una región y una instancia. Free sirve para una prueba, se duerme tras 15 minutos sin tráfico y tiene restricciones de SMTP. Para uso continuo selecciona una instancia de pago y revisa el precio que muestra Render antes de contratar. Si aparecen errores de memoria, aumenta los recursos según los registros del servicio.
6. Antes de desplegar, configura las variables del apartado siguiente.

## 3. Variables de entorno

En Environment agrega cada nombre y su valor, sin comillas adicionales:

| Variable | Valor |
| --- | --- |
| SPRING_PROFILES_ACTIVE | remote |
| DB_URL | Tu URL JDBC completa de SQL Server |
| DB_USERNAME | Usuario SQL de la aplicación |
| DB_PASSWORD | Contraseña de ese usuario |
| MIGRATION_USERNAME | Usuario SQL con permisos de migración |
| MIGRATION_PASSWORD | Contraseña de ese usuario |
| NOTIFICATIONS_ENABLED | false |
| CORS_ALLOWED_ORIGINS | https://reserva.cliente.atenix.net,https://reserva.admin.atenix.net |

Puedes consultar tus valores privados actuales en .env.remote localmente. No uses los marcadores YOUR_SQL_LOGIN o sql-server.example.invalid de la plantilla. Render no carga ese archivo automáticamente: debes introducir los valores en su panel.

Ejemplo de formato, NO una conexión real:

```text
jdbc:sqlserver://TU_SERVIDOR:1433;databaseName=TourismDemo;encrypt=true;trustServerCertificate=false;loginTimeout=10
```

Usa el nombre y el certificado reales del servidor. La conexión privada de demostración utilizaba trustServerCertificate=true; eso omite validar la identidad del certificado. Para producción solicita un certificado confiable y configura false. No cambies este valor sin comprobar que el certificado del servidor lo permite.

No configures SERVER_SERVLET_SESSION_COOKIE_SECURE=false: la publicación utiliza HTTPS. No necesitas definir PORT: Render lo proporciona y el perfil remote lo lee.

## 4. Acceso a SQL Server y primer despliegue

1. Comprueba con el administrador de SQL Server que acepta conexiones desde el alojamiento. Que tu computadora se conecte no garantiza que Render pueda hacerlo.
2. Si hay un firewall, abre Connect > Outbound en el servicio Render y proporciona esos rangos de IP al administrador para autorizar el puerto SQL correspondiente (normalmente 1433). Si la base solo es accesible por una VPN/red interna, primero hay que resolver ese acceso.
3. Pulsa Deploy Web Service. Abre Logs y espera a que termine la compilación y el arranque de Spring Boot.
4. Copia la URL HTTPS asignada por Render (por ejemplo, un nombre terminado en .onrender.com).
5. Abre esa URL seguida de /api/v1/health. Debe responder JSON con status UP. La ruta raíz / puede rechazar el acceso: es normal, no es una página web.

La ruta de salud confirma que la aplicación responde; también debes comprobar /api/v1/public/business para verificar una lectura real de datos. No vuelvas a ejecutar scripts de creación ni cargar datos.

## 5. Configurar reserva.api.atenix.net

1. Dentro del servicio abre Settings > Custom Domains > Add Custom Domain.
2. Escribe reserva.api.atenix.net, sin https:// ni /api/v1.
3. Render mostrará el destino DNS que debes usar. Cópialo exactamente.
4. En el proveedor que administra la zona DNS atenix.net crea:

| Campo DNS | Valor |
| --- | --- |
| Tipo | CNAME |
| Nombre / Host | reserva.api (si el panel agrega atenix.net automáticamente) |
| Destino | El nombre .onrender.com que indique Render, sin https:// |
| TTL | Predeterminado |

Si el panel exige el nombre completo, usa reserva.api.atenix.net. No cambies los registros de reserva.cliente ni reserva.admin. Evita registros A/AAAA incompatibles para ese mismo nombre; sigue las instrucciones de Render para tu proveedor.

5. Regresa a Render y pulsa Verify. Espera la propagación DNS y la emisión del certificado.
6. Abre https://reserva.api.atenix.net/api/v1/health y verifica que funciona con HTTPS sin errores de certificado.

No pruebes el login de los frontend apuntándolos al dominio .onrender.com: la configuración utiliza cookies SameSite=Lax y los tres dominios HTTPS bajo atenix.net. El dominio personalizado forma parte de esta solución.

## 6. Actualizar ambos frontends en mediación

Los archivos publicados antes de estos cambios todavía llaman a su propio dominio. Hay que volver a compilarlos y publicarlos.

En cada repositorio usa las versiones de Node y npm indicadas en package.json y .nvmrc (actualmente Node 26.7.0 y npm 12.0.2):

```powershell
npm ci
npm run build
```

El build predeterminado es production y utiliza https://reserva.api.atenix.net/api/v1. Si mediación compila desde GitHub, actualiza su despliegue desde main con esos comandos. Si subes archivos manualmente, publica el CONTENIDO de:

- admin-web: dist/admin-web/browser/ en reserva.admin.atenix.net.
- customer-web: dist/customer-web/browser/ en reserva.cliente.atenix.net.

index.html debe quedar en la raíz web de cada sitio, junto con los JS, CSS y demás archivos de esa misma compilación. No publiques src, node_modules ni .env. Después limpia la caché del alojamiento/CDN si existe y recarga el navegador.

La URL está en src/environments/environment.production.ts de cada frontend. El archivo environment.ts conserva /api/v1 para npm start y el proxy de desarrollo. Cambiar .env.example o proxy.conf.json no cambia la URL de una compilación publicada.

## 7. Comprobar la conexión completa

1. Abre el sitio cliente y comprueba que aparecen el negocio, las tarifas y las fechas.
2. Abre el administrador e inicia sesión con un usuario existente. Las contraseñas de demostración deben haber sido sustituidas o los usuarios desactivados antes de exponer la aplicación.
3. En las herramientas del navegador (F12 > Network), verifica que las peticiones van a reserva.api.atenix.net/api/v1.
4. Comprueba navegación autenticada y cierre de sesión. Las pruebas que creen reservas o movimientos se deben hacer con datos de prueba autorizados.

Las peticiones envían cookies con credentials: include. Spring permite exactamente los dos orígenes configurados y conserva CSRF. La cookie continúa siendo Secure, HttpOnly y SameSite=Lax, sin compartirla con todos los subdominios. Los tres sitios deben utilizar HTTPS.

Usa una sola instancia de API al principio: actualmente las sesiones y los tokens CSRF se guardan en memoria. Un reinicio o despliegue exige volver a iniciar sesión. Varias réplicas requieren resolver primero el almacenamiento compartido de sesiones.

## Problemas frecuentes

| Síntoma | Qué revisar |
| --- | --- |
| Compilación Docker fallida | Logs de Render; confirmar main, Dockerfile y raíz vacía |
| Timeout SQL | Firewall, IP de salida, puerto, acceso público/VPN |
| Login failed for user | Usuario y contraseña SQL en Environment |
| Error de certificado SQL | Certificado, nombre DNS y trustServerCertificate |
| Error de validación Flyway | Base correcta e historial de migraciones; no ejecutar repair/baseline a ciegas |
| El frontend llama a su propio dominio | Publicar el build nuevo de producción y limpiar caché |
| Error CORS | Orígenes HTTPS exactos, sin barra final, y API accesible |
| Login devuelve CSRF_INVALID | Dominio personalizado, cookies, recargar tras reinicio y revisar solicitud /auth/csrf |
| Primera carga lenta | La instancia Free puede estar despertando |
| No salen correos | NOTIFICATIONS_ENABLED=false intencionalmente; configurar SMTP y revisar restricciones del plan |

## Documentación oficial

- https://render.com/docs/docker
- https://render.com/docs/custom-domains
- https://render.com/docs/outbound-ip-addresses
- https://render.com/docs/free
