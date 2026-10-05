# business-api

Servicios centrales del sistema de gestión turística. Este proyecto conecta el portal de visitantes y el panel del personal para mantener una gestión coherente de reservas, visitas y actividad del negocio.

## Qué gestiona

- Información del negocio, servicios, horarios, cupos y tarifas.
- Disponibilidad y reservas de visitantes.
- Registro de visitas y cobros.
- Gastos, devoluciones, ajustes y cierres de caja.
- Reportes de jornada en PDF y Excel.
- Notificaciones relacionadas con reservas y cierres.
- Calendario, tareas compartidas y registro de actividad.
- Acceso a funciones según el rol del personal.

## Propósito

Centralizar la información del negocio y aplicar las reglas de funcionamiento que comparten las distintas partes del sistema. Esto permite coordinar la atención de visitantes, el trabajo del equipo y el seguimiento administrativo.

## Parte del sistema

El sistema se compone de tres proyectos complementarios:

- customer-web: portal público de información y reservas.
- admin-web: panel de gestión para el personal y los administradores.
- business-api: servicios centrales que conectan ambas experiencias.

## Publicación

Consulta [la guía paso a paso para Render](docs/render-deployment.md) para desplegar la API y conectar los dominios del cliente y del administrador.
