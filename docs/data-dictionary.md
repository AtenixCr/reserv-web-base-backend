# Diccionario de datos — fase 1

SQL Server 2025 (17.0.1000.7). Todos los objetos pertenecen a `dbo`. No hay borrados en cascada. Los instantes son UTC; las fechas operativas corresponden a Costa Rica. Importes CRC exactos, sin conversiones contables.

Cada definición siguiente especifica tipo, nulabilidad, valor inicial y restricciones. `IDENTITY` genera claves internas; no se usa como código público. `rowversion` es un valor binario de concurrencia, no una fecha. La capa de aplicación comparará la versión enviada en las fases funcionales.

## BusinessSettings

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| singleton | `singleton tinyint NOT NULL DEFAULT 1 UNIQUE CHECK (singleton = 1)` |
| defaultCapacity | `defaultCapacity int NOT NULL CHECK (defaultCapacity >= 0)` |
| phone | `phone nvarchar(40) NOT NULL` |
| email | `email nvarchar(254) NOT NULL` |
| address | `address nvarchar(500) NOT NULL` |
| sinpeNumber | `sinpeNumber nvarchar(40) NOT NULL` |
| enabled | `enabled bit NOT NULL DEFAULT 0` |
| version | `version rowversion NOT NULL` |

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## BusinessTranslation

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| settingsId | `settingsId bigint NOT NULL REFERENCES dbo.BusinessSettings(id)` |
| locale | `locale varchar(2) NOT NULL DEFAULT 'es' CHECK (locale IN ('es','en','pt'))` |
| displayName | `displayName nvarchar(120) NOT NULL` |
| description | `description nvarchar(4000) NOT NULL` |
| services | `services nvarchar(4000) NOT NULL` |
| version | `version rowversion NOT NULL` |

Restricciones de tabla:

- `UNIQUE (settingsId, locale)`

## BusinessHour

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| settingsId | `settingsId bigint NOT NULL REFERENCES dbo.BusinessSettings(id)` |
| weekday | `weekday tinyint NOT NULL CHECK (weekday BETWEEN 1 AND 7)` |
| opensAt | `opensAt time(0) NOT NULL` |
| closesAt | `closesAt time(0) NOT NULL` |
| version | `version rowversion NOT NULL` |

Restricciones de tabla:

- `CHECK (opensAt < closesAt)`
- `UNIQUE (settingsId, weekday, opensAt)`

## ClosedDate

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| visitDate | `visitDate date NOT NULL UNIQUE` |
| reason | `reason nvarchar(500) NOT NULL` |
| version | `version rowversion NOT NULL` |

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## VisitDay

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| visitDate | `visitDate date NOT NULL UNIQUE` |
| capacityOverride | `capacityOverride int NULL CHECK (capacityOverride >= 0)` |
| version | `version rowversion NOT NULL` |

## RatePlan

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| adultPrice | `adultPrice decimal(19,2) NOT NULL CHECK (adultPrice >= 0)` |
| childPrice | `childPrice decimal(19,2) NOT NULL CHECK (childPrice >= 0)` |
| seniorPrice | `seniorPrice decimal(19,2) NOT NULL CHECK (seniorPrice >= 0)` |
| parkingPrice | `parkingPrice decimal(19,2) NOT NULL CHECK (parkingPrice >= 0)` |
| effectiveAt | `effectiveAt datetime2(7) NOT NULL` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |

## ExchangeRate

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| currency | `currency varchar(3) NOT NULL UNIQUE CHECK (currency IN ('USD','BRL'))` |
| rate | `rate decimal(19,8) NOT NULL CHECK (rate > 0)` |
| updatedAt | `updatedAt datetime2(7) NOT NULL` |
| version | `version rowversion NOT NULL` |

## AppUser

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| username | `username nvarchar(80) NOT NULL UNIQUE` |
| passwordHash | `passwordHash varchar(100) NOT NULL` |
| name | `name nvarchar(120) NOT NULL` |
| email | `email nvarchar(254) NOT NULL` |
| phone | `phone nvarchar(40) NOT NULL` |
| role | `role varchar(5) NOT NULL CHECK (role IN ('ADMIN','STAFF'))` |
| active | `active bit NOT NULL DEFAULT 1` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |
| version | `version rowversion NOT NULL` |

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## Reservation

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| code | `code varchar(40) NOT NULL UNIQUE` |
| visitDayId | `visitDayId bigint NOT NULL REFERENCES dbo.VisitDay(id)` |
| ratePlanId | `ratePlanId bigint NOT NULL REFERENCES dbo.RatePlan(id)` |
| name | `name nvarchar(120) NOT NULL` |
| email | `email nvarchar(254) NOT NULL` |
| phone | `phone nvarchar(40) NOT NULL` |
| note | `note nvarchar(1000) NULL` |
| adults | `adults int NOT NULL CHECK (adults >= 0)` |
| children | `children int NOT NULL CHECK (children >= 0)` |
| seniors | `seniors int NOT NULL CHECK (seniors >= 0)` |
| vehicles | `vehicles int NOT NULL CHECK (vehicles >= 0)` |
| adultPrice | `adultPrice decimal(19,2) NOT NULL CHECK (adultPrice >= 0)` |
| childPrice | `childPrice decimal(19,2) NOT NULL CHECK (childPrice >= 0)` |
| seniorPrice | `seniorPrice decimal(19,2) NOT NULL CHECK (seniorPrice >= 0)` |
| parkingPrice | `parkingPrice decimal(19,2) NOT NULL CHECK (parkingPrice >= 0)` |
| currency | `currency varchar(3) NOT NULL DEFAULT 'CRC' CHECK (currency = 'CRC')` |
| total | `total decimal(19,2) NOT NULL CHECK (total >= 0)` |
| locale | `locale varchar(2) NOT NULL DEFAULT 'es' CHECK (locale IN ('es','en','pt'))` |
| status | `status varchar(10) NOT NULL CHECK (status IN ('CONFIRMED','CHECKED_IN','CANCELLED','NO_SHOW'))` |
| statusReason | `statusReason nvarchar(1000) NULL` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |
| version | `version rowversion NOT NULL` |

Restricciones de tabla:

- `CHECK (CAST(adults AS bigint) + children + seniors > 0)`
- `CHECK (total = CAST(adults AS decimal(19,2)) * adultPrice + CAST(children AS decimal(19,2)) * childPrice + CAST(seniors AS decimal(19,2)) * seniorPrice + CAST(vehicles AS decimal(19,2)) * parkingPrice)`

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## Admission

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| code | `code varchar(40) NOT NULL UNIQUE` |
| visitDayId | `visitDayId bigint NOT NULL REFERENCES dbo.VisitDay(id)` |
| reservationId | `reservationId bigint NULL REFERENCES dbo.Reservation(id)` |
| name | `name nvarchar(120) NULL` |
| email | `email nvarchar(254) NULL` |
| phone | `phone nvarchar(40) NULL` |
| note | `note nvarchar(1000) NULL` |
| adults | `adults int NOT NULL CHECK (adults >= 0)` |
| children | `children int NOT NULL CHECK (children >= 0)` |
| seniors | `seniors int NOT NULL CHECK (seniors >= 0)` |
| vehicles | `vehicles int NOT NULL CHECK (vehicles >= 0)` |
| adultPrice | `adultPrice decimal(19,2) NOT NULL CHECK (adultPrice >= 0)` |
| childPrice | `childPrice decimal(19,2) NOT NULL CHECK (childPrice >= 0)` |
| seniorPrice | `seniorPrice decimal(19,2) NOT NULL CHECK (seniorPrice >= 0)` |
| parkingPrice | `parkingPrice decimal(19,2) NOT NULL CHECK (parkingPrice >= 0)` |
| currency | `currency varchar(3) NOT NULL DEFAULT 'CRC' CHECK (currency = 'CRC')` |
| actorId | `actorId bigint NOT NULL REFERENCES dbo.AppUser(id)` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |
| version | `version rowversion NOT NULL` |

Restricciones de tabla:

- `CHECK (CAST(adults AS bigint) + children + seniors > 0)`

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## CashSession

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| businessDate | `businessDate date NOT NULL UNIQUE` |
| openingFloat | `openingFloat decimal(19,2) NOT NULL CHECK (openingFloat >= 0)` |
| status | `status varchar(6) NOT NULL CHECK (status IN ('OPEN','CLOSED'))` |
| openedBy | `openedBy bigint NOT NULL REFERENCES dbo.AppUser(id)` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |
| version | `version rowversion NOT NULL` |

## Payment

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| admissionId | `admissionId bigint NOT NULL UNIQUE REFERENCES dbo.Admission(id)` |
| cashSessionId | `cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id)` |
| method | `method varchar(5) NOT NULL CHECK (method IN ('CASH','SINPE','CARD'))` |
| amount | `amount decimal(19,2) NOT NULL CHECK (amount > 0)` |
| receivedAmount | `receivedAmount decimal(19,2) NULL` |
| changeAmount | `changeAmount decimal(19,2) NULL` |
| reference | `reference nvarchar(120) NULL` |
| actorId | `actorId bigint NOT NULL REFERENCES dbo.AppUser(id)` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |

Restricciones de tabla:

- `CHECK ((method = 'CASH' AND receivedAmount IS NOT NULL AND changeAmount IS NOT NULL AND receivedAmount >= amount AND changeAmount = receivedAmount - amount) OR (method <> 'CASH' AND receivedAmount IS NULL AND changeAmount IS NULL))`

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## Expense

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| cashSessionId | `cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id)` |
| method | `method varchar(5) NOT NULL CHECK (method IN ('CASH','SINPE','CARD'))` |
| amount | `amount decimal(19,2) NOT NULL CHECK (amount > 0)` |
| category | `category nvarchar(120) NOT NULL` |
| description | `description nvarchar(1000) NOT NULL` |
| actorId | `actorId bigint NOT NULL REFERENCES dbo.AppUser(id)` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## AdmissionRevision

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| admissionId | `admissionId bigint NOT NULL REFERENCES dbo.Admission(id)` |
| previousAdults | `previousAdults int NOT NULL CHECK (previousAdults >= 0)` |
| previousChildren | `previousChildren int NOT NULL CHECK (previousChildren >= 0)` |
| previousSeniors | `previousSeniors int NOT NULL CHECK (previousSeniors >= 0)` |
| previousVehicles | `previousVehicles int NOT NULL CHECK (previousVehicles >= 0)` |
| adults | `adults int NOT NULL CHECK (adults >= 0)` |
| children | `children int NOT NULL CHECK (children >= 0)` |
| seniors | `seniors int NOT NULL CHECK (seniors >= 0)` |
| vehicles | `vehicles int NOT NULL CHECK (vehicles >= 0)` |
| difference | `difference decimal(19,2) NOT NULL` |
| reason | `reason nvarchar(1000) NOT NULL` |
| actorId | `actorId bigint NOT NULL REFERENCES dbo.AppUser(id)` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |

Restricciones de tabla:

- `CHECK (CAST(adults AS bigint) + children + seniors > 0)`

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## Refund

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| admissionId | `admissionId bigint NOT NULL REFERENCES dbo.Admission(id)` |
| paymentId | `paymentId bigint NULL REFERENCES dbo.Payment(id)` |
| revisionId | `revisionId bigint NULL REFERENCES dbo.AdmissionRevision(id)` |
| cashSessionId | `cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id)` |
| method | `method varchar(5) NOT NULL CHECK (method IN ('CASH','SINPE','CARD'))` |
| amount | `amount decimal(19,2) NOT NULL CHECK (amount > 0)` |
| reason | `reason nvarchar(1000) NOT NULL` |
| actorId | `actorId bigint NOT NULL REFERENCES dbo.AppUser(id)` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## Adjustment

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| cashSessionId | `cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id)` |
| admissionId | `admissionId bigint NULL REFERENCES dbo.Admission(id)` |
| revisionId | `revisionId bigint NULL REFERENCES dbo.AdmissionRevision(id)` |
| expenseId | `expenseId bigint NULL REFERENCES dbo.Expense(id)` |
| refundId | `refundId bigint NULL REFERENCES dbo.Refund(id)` |
| originalAdjustmentId | `originalAdjustmentId bigint NULL REFERENCES dbo.Adjustment(id)` |
| kind | `kind varchar(20) NOT NULL CHECK (kind IN ('ADDITIONAL_PAYMENT','EXPENSE_COMPENSATION','REFUND_COMPENSATION','ADJUSTMENT_REVERSAL'))` |
| method | `method varchar(5) NOT NULL CHECK (method IN ('CASH','SINPE','CARD'))` |
| amount | `amount decimal(19,2) NOT NULL CHECK (amount <> 0)` |
| reason | `reason nvarchar(1000) NOT NULL` |
| actorId | `actorId bigint NOT NULL REFERENCES dbo.AppUser(id)` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |

Restricciones de tabla:

- `CHECK ((kind = 'ADDITIONAL_PAYMENT' AND admissionId IS NOT NULL AND revisionId IS NOT NULL AND amount > 0 AND expenseId IS NULL AND refundId IS NULL AND originalAdjustmentId IS NULL) OR (kind = 'EXPENSE_COMPENSATION' AND expenseId IS NOT NULL AND amount > 0 AND refundId IS NULL AND originalAdjustmentId IS NULL) OR (kind = 'REFUND_COMPENSATION' AND refundId IS NOT NULL AND amount > 0 AND expenseId IS NULL AND originalAdjustmentId IS NULL) OR (kind = 'ADJUSTMENT_REVERSAL' AND originalAdjustmentId IS NOT NULL AND expenseId IS NULL AND refundId IS NULL))`

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## CashClosure

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| cashSessionId | `cashSessionId bigint NOT NULL UNIQUE REFERENCES dbo.CashSession(id)` |
| countedCash | `countedCash decimal(19,2) NOT NULL CHECK (countedCash >= 0)` |
| expectedCash | `expectedCash decimal(19,2) NOT NULL` |
| difference | `difference decimal(19,2) NOT NULL` |
| operatingResult | `operatingResult decimal(19,2) NOT NULL` |
| note | `note nvarchar(1000) NULL` |
| snapshot | `snapshot nvarchar(max) NOT NULL CHECK (ISJSON(snapshot) = 1)` |
| recipient | `recipient nvarchar(254) NOT NULL` |
| locale | `locale varchar(2) NOT NULL DEFAULT 'es' CHECK (locale IN ('es','en','pt'))` |
| actorId | `actorId bigint NOT NULL REFERENCES dbo.AppUser(id)` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |

Restricciones de tabla:

- `CHECK (difference = countedCash - expectedCash)`
- `CHECK (difference = 0 OR (note IS NOT NULL AND LEN(LTRIM(RTRIM(note))) > 0))`

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## Task

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| taskDate | `taskDate date NOT NULL` |
| title | `title nvarchar(120) NOT NULL CHECK (LEN(LTRIM(RTRIM(title))) >= 1)` |
| description | `description nvarchar(2000) NOT NULL CHECK (LEN(LTRIM(RTRIM(description))) >= 1)` |
| status | `status varchar(9) NOT NULL CHECK (status IN ('PENDING','COMPLETED'))` |
| authorId | `authorId bigint NOT NULL REFERENCES dbo.AppUser(id)` |
| updatedBy | `updatedBy bigint NOT NULL REFERENCES dbo.AppUser(id)` |
| updatedAt | `updatedAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |
| version | `version rowversion NOT NULL` |

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## EmailOutbox

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| eventType | `eventType varchar(40) NOT NULL` |
| eventId | `eventId bigint NOT NULL` |
| eventVersion | `eventVersion int NOT NULL CHECK (eventVersion >= 1)` |
| recipient | `recipient nvarchar(254) NOT NULL` |
| locale | `locale varchar(2) NOT NULL DEFAULT 'es' CHECK (locale IN ('es','en','pt'))` |
| payload | `payload nvarchar(max) NOT NULL CHECK (ISJSON(payload) = 1)` |
| messageId | `messageId varchar(120) NOT NULL UNIQUE` |
| status | `status varchar(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PROCESSING','SENT','FAILED'))` |
| attempts | `attempts int NOT NULL DEFAULT 0 CHECK (attempts >= 0)` |
| availableAt | `availableAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |
| lockedAt | `lockedAt datetime2(7) NULL` |
| sentAt | `sentAt datetime2(7) NULL` |
| lastErrorCode | `lastErrorCode varchar(80) NULL` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |
| version | `version rowversion NOT NULL` |

Restricciones de tabla:

- `UNIQUE (eventType, eventId, eventVersion)`

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## EmailAttempt

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| outboxId | `outboxId bigint NOT NULL REFERENCES dbo.EmailOutbox(id)` |
| attemptNumber | `attemptNumber int NOT NULL CHECK (attemptNumber >= 1)` |
| result | `result varchar(20) NOT NULL` |
| errorCode | `errorCode varchar(80) NULL` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |

Restricciones de tabla:

- `UNIQUE (outboxId, attemptNumber)`

## IdempotencyRecord

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| operation | `operation varchar(80) NOT NULL` |
| keyHash | `keyHash char(64) NOT NULL` |
| requestHash | `requestHash char(64) NOT NULL` |
| responseStatus | `responseStatus int NOT NULL CHECK (responseStatus BETWEEN 200 AND 599)` |
| responseBody | `responseBody nvarchar(max) NOT NULL CHECK (ISJSON(responseBody) = 1)` |
| createdAt | `createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |
| expiresAt | `expiresAt datetime2(7) NOT NULL` |

Restricciones de tabla:

- `UNIQUE (operation, keyHash)`
- `CHECK (expiresAt >= DATEADD(hour, 48, createdAt))`

Los datos de contacto, textos libres y secretos se excluyen de las instantáneas de auditoría. Un cambio en esas columnas sí genera un evento, pero no copia su contenido.

## AuditLog

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| entity | `entity varchar(80) NOT NULL` |
| entityId | `entityId bigint NOT NULL` |
| operation | `operation varchar(6) NOT NULL CHECK (operation IN ('INSERT','UPDATE','DELETE'))` |
| occurredAt | `occurredAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |
| actorId | `actorId bigint NULL REFERENCES dbo.AppUser(id)` |
| origin | `origin varchar(12) NOT NULL CHECK (origin IN ('APPLICATION','EXTERNAL'))` |
| requestId | `requestId uniqueidentifier NULL` |
| beforeData | `beforeData nvarchar(max) NULL CHECK (ISJSON(beforeData) = 1)` |
| afterData | `afterData nvarchar(max) NULL CHECK (ISJSON(afterData) = 1)` |

## ActivityEvent

| Columna | Definición SQL |
| --- | --- |
| id | `id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY` |
| eventType | `eventType varchar(80) NOT NULL` |
| entity | `entity varchar(80) NULL` |
| entityId | `entityId bigint NULL` |
| actorId | `actorId bigint NULL REFERENCES dbo.AppUser(id)` |
| requestId | `requestId uniqueidentifier NULL` |
| outcome | `outcome varchar(20) NOT NULL` |
| occurredAt | `occurredAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()` |

## Índices

- `CREATE UNIQUE INDEX UX_Admission_Reservation ON dbo.Admission(reservationId) WHERE reservationId IS NOT NULL;`
- `CREATE UNIQUE INDEX UX_CashSession_Open ON dbo.CashSession(status) WHERE status = 'OPEN';`
- `CREATE UNIQUE INDEX UX_Refund_Revision ON dbo.Refund(revisionId) WHERE revisionId IS NOT NULL;`
- `CREATE UNIQUE INDEX UX_Adjustment_Revision ON dbo.Adjustment(revisionId) WHERE revisionId IS NOT NULL;`
- `CREATE INDEX IX_Reservation_Day_Status ON dbo.Reservation(visitDayId, status) INCLUDE (adults, children, seniors);`
- `CREATE INDEX IX_Reservation_Name ON dbo.Reservation(name);`
- `CREATE INDEX IX_Admission_Day ON dbo.Admission(visitDayId) INCLUDE (adults, children, seniors, vehicles);`
- `CREATE INDEX IX_Admission_Name ON dbo.Admission(name);`
- `CREATE INDEX IX_RatePlan_Effective ON dbo.RatePlan(effectiveAt DESC, id DESC);`
- `CREATE INDEX IX_Task_Date_Status ON dbo.Task(taskDate, status);`
- `CREATE INDEX IX_Outbox_Work ON dbo.EmailOutbox(status, availableAt) INCLUDE (lockedAt, attempts);`
- `CREATE INDEX IX_Idempotency_Expiry ON dbo.IdempotencyRecord(expiresAt);`
- `CREATE INDEX IX_AuditLog_Time ON dbo.AuditLog(occurredAt DESC, id DESC);`
- `CREATE INDEX IX_AuditLog_Request ON dbo.AuditLog(requestId);`
- `CREATE INDEX IX_AuditLog_Actor ON dbo.AuditLog(actorId, occurredAt DESC);`
- `CREATE INDEX IX_AuditLog_Entity ON dbo.AuditLog(entity, entityId, occurredAt DESC);`
- `CREATE INDEX IX_ActivityEvent_Time ON dbo.ActivityEvent(occurredAt DESC, id DESC);`
- `CREATE INDEX IX_Payment_Session ON dbo.Payment(cashSessionId);`
- `CREATE INDEX IX_Expense_Session ON dbo.Expense(cashSessionId);`
- `CREATE INDEX IX_Refund_Session ON dbo.Refund(cashSessionId);`
- `CREATE INDEX IX_Adjustment_Session ON dbo.Adjustment(cashSessionId);`
- `CREATE INDEX IX_Refund_Admission ON dbo.Refund(admissionId);`
- `CREATE INDEX IX_Adjustment_Admission ON dbo.Adjustment(admissionId);`
- `CREATE INDEX IX_AdmissionRevision_Admission ON dbo.AdmissionRevision(admissionId);`

## Invariantes transaccionales pendientes de sus fases

El esquema protege tipos, relaciones, estados, importes, unicidad e inmutabilidad. La disponibilidad y los cambios de fecha requieren bloqueo de VisitDay; cobro y cierre requieren bloqueo de CashSession; las devoluciones requieren bloqueo del ingreso original. Las comprobaciones de horario, fecha actual, permisos, saldo, transición de reserva, tarifas y conflictos de configuración pertenecen a servicios transaccionales que se implementan en fases 2–5. La existencia del esquema no declara estas funciones terminadas.

AuditLog y ActivityEvent no tienen triggers. IdempotencyRecord es infraestructura: no audita claves ni respuestas privadas. Los usuarios de aplicación no pueden modificar ni eliminar auditoría. EmailAttempt conserva el resultado seguro de cada intento; no almacena respuestas SMTP sin filtrar.

## Uso en fase 2

Sin tablas nuevas. V4 agrega únicamente el permiso local DELETE sobre ExchangeRate para desactivar una conversión. BusinessSettings.rowVersion protege el conjunto editable y también la publicación de tarifas y tasas. Las fechas históricas se conservan. AppUser se consulta con Spring Data JPA y no se expone como entidad. EmailOutbox recibe eventos HOURS_CHANGED transaccionales con idioma y versión; su entrega no está implementada en esta fase.

## Uso en fase 3

No se requieren nuevas tablas, migraciones ni inserts de instalación: Reservation, VisitDay, IdempotencyRecord, EmailOutbox y EmailAttempt ya existen. La API utiliza los permisos limitados de aplicación de las migraciones anteriores. El snapshot de confirmación se guarda tanto en IdempotencyRecord.responseBody como en EmailOutbox.payload, por lo que ambos requieren protección y política de retención para datos de contacto. No se copian a AuditLog.

IdempotencyRecord.operation usa CREATE_RESERVATION; keyHash y requestHash son SHA-256, responseStatus es 201 y expiresAt se fija a 49 horas. No hay borrado automático todavía. EmailOutbox.attempts se incrementa al reclamar el trabajo; EmailAttempt se agrega al terminar o recuperar un arrendamiento vencido, nunca se actualiza. Message-ID no cambia con los reintentos. Las transiciones administrativas utilizan rowversion y no borran reservas.


## Uso en fase 4

V5 agrega a Adjustment `receivedAmount decimal(19,2) NULL`, `changeAmount decimal(19,2) NULL` y `reference nvarchar(120) NULL`. Su restricción permite ambos importes nulos para filas históricas o exige ADDITIONAL_PAYMENT en efectivo, recibido suficiente y vuelto igual a recibido menos amount. El servicio exige recibo en todo nuevo adicional CASH. La referencia se excluye del contenido de auditoría, pero cambiarla sigue produciendo evento. Los importes sí forman parte de la lista explícita auditable. La inmutabilidad de Adjustment se conserva.

Admission copia cuatro tarifas y conserva cantidades operativas actuales. Payment es único por ingreso, con monto positivo; las entradas gratuitas no crean Payment. AdmissionRevision conserva antes/después, diferencia, motivo y actor. Adjustment y Refund enlazan la revisión y la jornada actual; Admission conserva su VisitDay original. IdempotencyRecord usa ADMISSION:actorId y CORRECTION:actorId:admissionId, 49 horas mínimas, sin purga automática. Las respuestas privadas requieren la misma protección que los contactos.

Las escrituras de esta fase bloquean primero BusinessSettings, después VisitDay y finalmente CashSession cuando hay movimiento. Todas las verificaciones e inserciones de cobro se revierten juntas. La concurrencia con el cierre se completará y probará en fase 5. No se agregan tablas ni inserts de instalación.

## Uso en fase 5

V6 crea UX_Adjustment_Reversal, índice UNIQUE sobre `originalAdjustmentId WHERE originalAdjustmentId IS NOT NULL`. Impide reversiones duplicadas sin restringir compensaciones parciales históricas. Expense y Refund se compensan por el saldo pendiente calculado sobre su cadena de ajustes y reversiones; no se actualizan ni eliminan movimientos. Los bloqueos de configuración y caja serializan devolución, compensación y cierre.

CashClosure.snapshot guarda JSON schemaVersion 2: businessDate, timezone, currency, cashSessionId, openingFloat, arrays payments/expenses/refunds/adjustments/attendance, totals, methods y metadatos closedAt/locale/actorId/actorName/recipient/businessName/countedCash/difference/note. Cada pago incluye las cuatro tarifas copiadas del ingreso y su fecha de visita. La asistencia incluye cantidades/tarifas y fecha original. Identificadores e importes son cadenas; cantidades enteras. Los importes de conciliación pueden ser negativos. El id de cierre se añade al DTO al leer, sin alterar la instantánea persistida.

CashClosure conserva además columnas indexables de importes, destinatario, idioma, actor y createdAt. Una única fila por CashSession y triggers de inmutabilidad impiden reemplazos. CashSession.rowversion cambia ante movimientos o cambios que afectan el resumen abierto. CashClosure y EmailOutbox CASH_CLOSED se insertan en una sola transacción; eventId identifica el cierre y eventVersion es 1. La carga contiene closureId y businessDate; los adjuntos se regeneran desde la instantánea, nunca desde tablas operativas actuales.

IdempotencyRecord usa EXPENSE:actorId, REFUND:actorId, COMPENSATION:actorId y CLOSE:actorId:sessionId. Conservación mínima de 49 horas y misma protección de datos que los movimientos. ActivityEvent incorpora CASH_CLOSED, REPORT_EXPORTED y EMAIL_MANUAL_RETRY. No hay nuevas tablas ni ampliación de permisos de aplicación.

## Uso en fase 6

Task mantiene fecha, título, descripción, estado, autor, último editor, instantes UTC y rowversion. Sus cambios se auditan mediante los triggers existentes, incluida la eliminación. No existen asignaciones, recurrencias ni alarmas. ActivityEvent incorpora índices por requestId, actor/fecha y entidad/fecha (V7). La lectura de actividad combina eventos y auditoría por requestId sin devolver los documentos JSON internos. No se crean tablas nuevas.
