-- Test fixture only: never deploy or execute against TourismDemo.
IF DB_NAME() <> 'TourismIntegrationTest' OR @@TRANCOUNT = 0
    THROW 51000, 'Fixture requires an isolated integration database and a transaction', 1;
IF COALESCE(CONVERT(nvarchar(20), SESSION_CONTEXT(N'environment')), '') <> 'integration-test'
    THROW 51000, 'Fixture requires explicit integration-test context', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.AppUser WHERE username = 'admin.test')
    THROW 51000, 'The test must create its BCrypt user first', 1;
IF EXISTS (SELECT 1 FROM dbo.BusinessSettings)
    THROW 51000, 'Demo seed already applied', 1;
DECLARE @today date = CONVERT(date, SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'Central America Standard Time');
DECLARE @admin bigint = (SELECT id FROM dbo.AppUser WHERE username = 'admin.test');
INSERT dbo.BusinessSettings(defaultCapacity, phone, email, address, sinpeNumber, enabled)
VALUES (100, '+506 0000 0000', 'demo@example.invalid', N'Dirección ficticia, Costa Rica', '00000000', 0);
DECLARE @settings bigint = SCOPE_IDENTITY();
INSERT dbo.BusinessTranslation(settingsId, locale, displayName, description, services)
VALUES (@settings, 'es', N'Parque de demostración', N'Datos ficticios para desarrollo local.', N'Senderos y zonas de descanso'),
(@settings, 'en', N'Demonstration park', N'Fictitious data for local development.', N'Trails and rest areas'),
(@settings, 'pt', N'Parque de demonstração', N'Dados fictícios para desenvolvimento local.', N'Trilhas e áreas de descanso');
INSERT dbo.BusinessHour(settingsId, weekday, opensAt, closesAt)
SELECT @settings, dayNumber, '08:00', '17:00' FROM (VALUES (1),(2),(3),(4),(5),(6),(7)) days(dayNumber);
INSERT dbo.RatePlan(adultPrice, childPrice, seniorPrice, parkingPrice, effectiveAt)
VALUES (5000,2500,3500,1000,SYSUTCDATETIME());
DECLARE @rate bigint = SCOPE_IDENTITY();
INSERT dbo.ExchangeRate(currency, rate, updatedAt) VALUES ('USD',0.00200000,SYSUTCDATETIME()),('BRL',0.01000000,SYSUTCDATETIME());
INSERT dbo.VisitDay(visitDate, capacityOverride)
VALUES (DATEADD(day,-1,@today),NULL),(@today,NULL),(DATEADD(day,1,@today),NULL),(DATEADD(day,2,@today),2),(DATEADD(day,3,@today),NULL);
DECLARE @past bigint = (SELECT id FROM dbo.VisitDay WHERE visitDate=DATEADD(day,-1,@today));
DECLARE @current bigint = (SELECT id FROM dbo.VisitDay WHERE visitDate=@today);
DECLARE @tomorrow bigint = (SELECT id FROM dbo.VisitDay WHERE visitDate=DATEADD(day,1,@today));
DECLARE @full bigint = (SELECT id FROM dbo.VisitDay WHERE visitDate=DATEADD(day,2,@today));
INSERT dbo.Reservation(code,visitDayId,ratePlanId,name,email,phone,adults,children,seniors,vehicles,adultPrice,childPrice,seniorPrice,parkingPrice,total,locale,status)
VALUES ('RES-'+CONVERT(varchar(36),NEWID()),@tomorrow,@rate,N'Visitante de prueba','visitor@example.invalid','00000000',2,1,1,1,5000,2500,3500,1000,17000,'es','CONFIRMED'),
('RES-'+CONVERT(varchar(36),NEWID()),@full,@rate,N'Grupo de prueba','full@example.invalid','00000000',2,0,0,0,5000,2500,3500,1000,10000,'en','CONFIRMED'),
('RES-'+CONVERT(varchar(36),NEWID()),@tomorrow,@rate,N'Cancelación de prueba','cancelled@example.invalid','00000000',1,0,0,0,5000,2500,3500,1000,5000,'pt','CANCELLED'),
('RES-'+CONVERT(varchar(36),NEWID()),@past,@rate,N'Ausencia de prueba','absent@example.invalid','00000000',1,0,0,0,5000,2500,3500,1000,5000,'es','NO_SHOW'),
('RES-'+CONVERT(varchar(36),NEWID()),@past,@rate,N'Ingreso de prueba','admitted@example.invalid','00000000',10,0,0,0,5000,2500,3500,1000,50000,'es','CHECKED_IN');
DECLARE @reservation bigint = SCOPE_IDENTITY();
INSERT dbo.CashSession(businessDate,openingFloat,status,openedBy) VALUES (DATEADD(day,-1,@today),20000,'OPEN',@admin);
DECLARE @historicalSession bigint = SCOPE_IDENTITY();
INSERT dbo.Admission(code,visitDayId,reservationId,name,email,phone,adults,children,seniors,vehicles,adultPrice,childPrice,seniorPrice,parkingPrice,actorId)
VALUES ('VIS-'+CONVERT(varchar(36),NEWID()),@past,@reservation,N'Ingreso de prueba','admitted@example.invalid','00000000',10,0,0,0,5000,2500,3500,1000,@admin);
DECLARE @cashAdmission bigint = SCOPE_IDENTITY();
INSERT dbo.Payment(admissionId,cashSessionId,method,amount,receivedAmount,changeAmount,actorId)
VALUES (@cashAdmission,@historicalSession,'CASH',50000,50000,0,@admin);
DECLARE @payment bigint = SCOPE_IDENTITY();
INSERT dbo.Admission(code,visitDayId,adults,children,seniors,vehicles,adultPrice,childPrice,seniorPrice,parkingPrice,actorId)
VALUES ('VIS-'+CONVERT(varchar(36),NEWID()),@past,6,0,0,0,5000,2500,3500,1000,@admin);
DECLARE @sinpeAdmission bigint = SCOPE_IDENTITY();
INSERT dbo.Payment(admissionId,cashSessionId,method,amount,actorId) VALUES (@sinpeAdmission,@historicalSession,'SINPE',30000,@admin);
INSERT dbo.Admission(code,visitDayId,adults,children,seniors,vehicles,adultPrice,childPrice,seniorPrice,parkingPrice,actorId)
VALUES ('VIS-'+CONVERT(varchar(36),NEWID()),@past,2,0,0,0,5000,2500,3500,1000,@admin);
DECLARE @cardAdmission bigint = SCOPE_IDENTITY();
INSERT dbo.Payment(admissionId,cashSessionId,method,amount,actorId) VALUES (@cardAdmission,@historicalSession,'CARD',10000,@admin);
INSERT dbo.Expense(cashSessionId,method,amount,category,description,actorId)
VALUES (@historicalSession,'CASH',5000,N'Supplies',N'Gasto ficticio',@admin);
DECLARE @expense bigint = SCOPE_IDENTITY();
INSERT dbo.Refund(admissionId,paymentId,cashSessionId,method,amount,reason,actorId)
VALUES (@cashAdmission,@payment,@historicalSession,'CASH',2000,N'Devolución comercial ficticia',@admin);
DECLARE @snapshot nvarchar(max) = (SELECT 1 AS schemaVersion, 'CRC' AS currency, CONVERT(varchar(10),DATEADD(day,-1,@today),23) AS businessDate, 'America/Costa_Rica' AS timeZone, @admin AS closedBy,
JSON_QUERY((SELECT * FROM dbo.Payment WHERE cashSessionId=@historicalSession FOR JSON PATH)) AS payments,
JSON_QUERY((SELECT id,cashSessionId,method,amount,category,actorId,createdAt FROM dbo.Expense WHERE cashSessionId=@historicalSession FOR JSON PATH)) AS expenses,
JSON_QUERY((SELECT id,admissionId,cashSessionId,method,amount,actorId,createdAt FROM dbo.Refund WHERE cashSessionId=@historicalSession FOR JSON PATH)) AS refunds,
JSON_QUERY('[]') AS adjustments,
JSON_QUERY((SELECT id,code,adults,children,seniors,vehicles,adultPrice,childPrice,seniorPrice,parkingPrice FROM dbo.Admission WHERE visitDayId=@past FOR JSON PATH)) AS attendance,
20000 AS openingFloat,63000 AS expectedCash,62500 AS countedCash,-500 AS difference,83000 AS operatingResult FOR JSON PATH,WITHOUT_ARRAY_WRAPPER);
INSERT dbo.CashClosure(cashSessionId,countedCash,expectedCash,difference,operatingResult,note,snapshot,recipient,locale,actorId)
VALUES (@historicalSession,62500,63000,-500,83000,N'Diferencia ficticia de demostración',@snapshot,'admin@example.invalid','es',@admin);
UPDATE dbo.CashSession SET status='CLOSED' WHERE id=@historicalSession;
INSERT dbo.CashSession(businessDate,openingFloat,status,openedBy) VALUES (@today,20000,'OPEN',@admin);
DECLARE @currentSession bigint = SCOPE_IDENTITY();
INSERT dbo.Adjustment(cashSessionId,expenseId,kind,method,amount,reason,actorId)
VALUES (@currentSession,@expense,'EXPENSE_COMPENSATION','CASH',1000,N'Corrección ficticia posterior al cierre',@admin);
INSERT dbo.Task(taskDate,title,description,status,authorId,updatedBy)
VALUES (@today,N'Revisar senderos',N'Tarea ficticia de demostración','PENDING',@admin,@admin),
(@today,N'Preparar recepción',N'Tarea ficticia completada','COMPLETED',@admin,@admin);
GO
