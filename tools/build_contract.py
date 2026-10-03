import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
ref = lambda name: {'$ref': '#/components/schemas/' + name}
string = {'type': 'string'}
date = {'type': 'string', 'format': 'date'}
array = lambda value: {'type': 'array', 'items': value}
obj = lambda properties, required=None: {'type': 'object', 'properties': properties, 'required': list(properties) if required is None else required}
schemas = {
    'Error': obj({'code': string, 'message': string, 'fieldErrors': {'type': 'object', 'additionalProperties': string}, 'traceId': string}),
    'Identity': obj({'id': string, 'username': string, 'name': string, 'role': {'type': 'string', 'enum': ['ADMIN', 'STAFF']}}),
    'Login': obj({'username': {'type': 'string', 'minLength': 1, 'maxLength': 80}, 'password': {'type': 'string', 'minLength': 1, 'maxLength': 200, 'writeOnly': True}}),
    'Csrf': obj({'headerName': string, 'token': string}),
    'Translation': obj({'locale': {'type': 'string', 'enum': ['es','en','pt']}, 'displayName': {'type':'string','minLength':1,'maxLength':120}, 'description': {'type':'string','maxLength':4000}, 'services': {'type':'string','maxLength':4000}}),
    'Hour': obj({'weekday': {'type':'integer','minimum':1,'maximum':7}, 'opensAt': {'type':'string','pattern':'^([01][0-9]|2[0-3]):[0-5][0-9]$'}, 'closesAt': {'type':'string','pattern':'^([01][0-9]|2[0-3]):[0-5][0-9]$'}}),
    'ClosedDate': obj({'visitDate': date, 'reason': {'type':'string','minLength':1,'maxLength':500}}),
    'CapacityOverride': obj({'visitDate': date, 'capacity': {'type':'integer','minimum':0}}),
    'Exchange': obj({'currency': {'type':'string','enum':['USD','BRL']}, 'rate': {'type':'string','pattern':r'^[0-9]{1,11}(\.[0-9]{1,8})?$'}, 'updatedOn':date}),
    'Money': {'type':'string','pattern':r'^[0-9]{1,17}(\.[0-9]{1,2})?$','example':'5000.00'},
}
schemas['Settings'] = obj({'version':string,'defaultCapacity':{'type':'integer','minimum':0},'phone':string,'email':{'type':'string','format':'email'},'address':string,'sinpeNumber':string,'enabled':{'type':'boolean'},'translations':array(ref('Translation')),'hours':array(ref('Hour')),'closedDates':array(ref('ClosedDate')),'capacityOverrides':array(ref('CapacityOverride'))})
price_fields = {name:ref('Money') for name in ['adultPrice','childPrice','seniorPrice','parkingPrice']}
schemas['Prices'] = obj({'version':string,**price_fields})
schemas['RatePlan'] = obj({'id':string,**price_fields})
schemas['Exchanges'] = obj({'version':string,'rates':array(ref('Exchange'))})
schemas['Snapshot'] = obj({'settings':{'allOf':[ref('Settings')],'nullable':True},'ratePlan':{'allOf':[ref('RatePlan')],'nullable':True},'exchangeRates':array(ref('Exchange')),'serverDate':date})
schemas['PublicBusiness'] = obj({'serverDate':date,'timeZone':string,'currency':{'type':'string','enum':['CRC']},'configured':{'type':'boolean'},'reservationsEnabled':{'type':'boolean','description':'Habilitado cuando el negocio tiene configuración activa, horarios y tarifas.'},'translations':array(ref('Translation')),'hours':array(ref('Hour')),'phone':string,'email':string,'address':string,'sinpeNumber':string,'ratePlan':{'allOf':[ref('RatePlan')],'nullable':True},'exchangeRates':array(ref('Exchange'))},['serverDate','timeZone','currency','configured','reservationsEnabled','ratePlan','exchangeRates'])
schemas['Dashboard'] = obj({'serverDate':date,'pendingReservations':{'type':'integer','format':'int64'},'admittedPeople':{'type':'integer','format':'int64'},'hours':array(ref('Hour')),'tasks':array(obj({'title':string,'status':{'type':'string','enum':['PENDING','COMPLETED']}}))})
paths = {}

def endpoint(path, method, operation, response, request=None, public=False):
    responses={'200':{'description':'Operación correcta','content':{'application/json':{'schema':response}}}}
    for status in ['400','401','403','409','429','500']:
        responses[status]={'description':'Error seguro traducible por code','content':{'application/json':{'schema':ref('Error')}}}
    responses['429']['headers']={'Retry-After':{'description':'Segundos hasta el siguiente intento','schema':{'type':'integer'}}}
    value={'operationId':operation,'responses':responses,'security':[] if public else [{'Session':[]}]}
    if method != 'get': value['parameters']=[{'in':'header','name':'X-CSRF-TOKEN','required':True,'schema':string,'description':'Obtener mediante GET /auth/csrf; renovar después de login.'}]
    if request: value['requestBody']={'required':True,'content':{'application/json':{'schema':ref(request)}}}
    paths.setdefault(path,{})[method]=value

endpoint('/health','get','getHealth',obj({'status':string,'phase':string}),public=True)
endpoint('/auth/csrf','get','getCsrf',ref('Csrf'),public=True)
endpoint('/auth/login','post','login',ref('Identity'),'Login',True)
endpoint('/auth/me','get','getIdentity',ref('Identity'))
endpoint('/auth/logout','post','logout',obj({'status':string}))
endpoint('/settings','get','getSettings',ref('Snapshot'))
endpoint('/settings','put','saveSettings',ref('Snapshot'),'Settings')
endpoint('/rate-plans','post','publishRates',ref('Snapshot'),'Prices')
endpoint('/exchange-rates','put','saveExchangeRates',ref('Snapshot'),'Exchanges')
endpoint('/public/business','get','getPublicBusiness',ref('PublicBusiness'),public=True)
endpoint('/dashboard','get','getDashboard',ref('Dashboard'))
for path in ['/settings','/rate-plans','/exchange-rates']:
    for value in paths[path].values(): value['description']='Solo ADMIN. version es rowversion codificado en Base64; usar new para configuración inicial. Un conflicto requiere recargar; nunca se sobrescribe silenciosamente. Cambios se auditan. El reemplazo de listas conserva fechas históricas.'
schemas['ReservationRequest']=obj({'visitDate':date,'name':{'type':'string','minLength':2,'maxLength':120},'email':{'type':'string','format':'email','maxLength':254},'phone':{'type':'string','maxLength':40},**{k:{'type':'integer','minimum':0,'maximum':2147483647} for k in ['adults','children','seniors','vehicles']},'note':{'type':'string','maxLength':1000,'nullable':True},'locale':{'type':'string','enum':['es','en','pt'],'default':'es'},'rateVersion':{'type':'string','nullable':True}},['visitDate','name','email','phone','adults','children','seniors','vehicles'])
schemas['AvailabilityDay']=obj({'visitDate':date,'hours':array(ref('Hour')),'available':{'type':'integer','format':'int64','minimum':0},'reason':{'type':'string','nullable':True,'enum':['OUT_OF_RANGE','NOT_CONFIGURED','BLOCKED','CLOSED','FULL']}})
schemas['AvailabilityMonth']=obj({'serverDate':date,'minDate':date,'maxDate':date,'days':array(ref('AvailabilityDay'))})
schemas['PriceLine']=obj({'category':{'type':'string','enum':['adults','children','seniors','vehicles']},'quantity':{'type':'integer'},'unitPrice':ref('Money'),'subtotal':ref('Money')})
schemas['ReservationQuote']=obj({'visitDate':date,'rateVersion':string,'currency':{'type':'string','enum':['CRC']},'lines':array(ref('PriceLine')),'total':ref('Money'),'hours':array(ref('Hour'))})
schemas['Confirmation']=obj({'id':string,'code':string,'status':string,'version':string,'name':string,'locale':string,'quote':ref('ReservationQuote'),'sinpeNumber':string,'businessName':string,'phone':string,'email':string,'emailStatus':{'type':'string','enum':['PENDING']}})
schemas['ReservationTransition']=obj({'version':string,'reason':{'type':'string','minLength':1,'maxLength':1000}})
endpoint('/public/availability','get','getAvailability',ref('AvailabilityMonth'),public=True)
paths['/public/availability']['get']['parameters']=[{'in':'query','name':'month','required':True,'schema':{'type':'string','pattern':'^[0-9]{4}-[0-9]{2}$'}}]
endpoint('/public/reservation-quotes','post','quoteReservation',ref('ReservationQuote'),'ReservationRequest',True)
endpoint('/public/reservations','post','createReservation',ref('Confirmation'),'ReservationRequest',True)
paths['/public/reservation-quotes']['post']['parameters']=[]
op=paths['/public/reservations']['post']
op['responses']['201']=op['responses'].pop('200')
op['parameters']=[{'in':'header','name':'Idempotency-Key','required':True,'schema':{'type':'string','format':'uuid'},'description':'UUID v4 aleatorio. Reutilizar con el mismo cuerpo al reintentar; no compartir. Asociación conservada al menos 48 horas.'}]
op['description']='Requiere rateVersion de la cotización. Cupo y tarifa se verifican al confirmar. Devuelve 409 si cambió la tarifa y exige nueva revisión. PENDING indica correo en cola; no confirma entrega. Límite inicial 10 intentos por IP en 10 minutos. Las rutas públicas no usan autenticación ni CSRF.'
for action in ['cancel','no-show']:
    path='/reservations/{id}/'+action
    endpoint(path,'post',action.replace('-','')+'Reservation',obj({'id':string,'status':string,'version':string}),'ReservationTransition')
    paths[path]['post']['parameters'].append({'in':'path','name':'id','required':True,'schema':{'type':'integer','format':'int64'}})
    paths[path]['post']['responses']['404']={'description':'Reserva inexistente','content':{'application/json':{'schema':ref('Error')}}}
    paths[path]['post']['description']='ADMIN o STAFF. Solo CONFIRMED y versión vigente. Motivo obligatorio. No-show exige fecha anterior o cierre del día transcurrido; cancelación libera cupo una sola vez.'
schemas['OpenCashSession']=obj({'openingFloat':ref('Money')})
schemas['CashSession']=obj({'id':string,'businessDate':date,'status':{'type':'string','enum':['OPEN','CLOSED']},'version':string,'canPay':{'type':'boolean'},'openingFloat':ref('Money')},['id','businessDate','status','version','canPay'])
schemas['CurrentCashSession']=obj({'serverDate':date,'session':{'allOf':[ref('CashSession')],'nullable':True}})
contact_fields={'name':{'type':'string','maxLength':120,'nullable':True},'email':{'type':'string','format':'email','maxLength':254,'nullable':True},'phone':{'type':'string','maxLength':40,'nullable':True},'note':{'type':'string','maxLength':1000,'nullable':True}}
counts={k:{'type':'integer','minimum':0,'maximum':2147483647} for k in ['adults','children','seniors','vehicles']}
payment_fields={'paymentMethod':{'type':'string','enum':['CASH','SINPE','CARD'],'nullable':True},'receivedAmount':{'allOf':[ref('Money')],'nullable':True},'reference':{'type':'string','maxLength':120,'nullable':True}}
schemas['AdmissionRequest']=obj({**contact_fields,**counts,**payment_fields,'reservationId':{'type':'string','pattern':'^[1-9][0-9]*$','nullable':True},'reservationVersion':{'type':'string','nullable':True},'rateVersion':{'type':'string','nullable':True}},list(counts))
schemas['AdmissionContact']=obj({'version':string,**contact_fields},['version'])
schemas['AdmissionCorrection']=obj({'version':string,**counts,'reason':{'type':'string','minLength':1,'maxLength':1000},**payment_fields},['version',*counts,'reason'])
schemas['Payment']=obj({'id':string,'method':string,'amount':ref('Money'),'receivedAmount':{'allOf':[ref('Money')],'nullable':True},'changeAmount':{'allOf':[ref('Money')],'nullable':True},'reference':{'type':'string','nullable':True}})
schemas['AdmissionRevision']=obj({'id':string,**counts,'difference':string,'reason':string,'createdAt':{'type':'string','format':'date-time'}})
schemas['Group']=obj({'id':string,'code':string,'visitDate':date,'version':string,'status':string,**contact_fields,**counts,'locale':string,'reservationId':{'type':'string','nullable':True},'quote':ref('ReservationQuote'),'payment':{'allOf':[ref('Payment')],'nullable':True},'revisions':array(ref('AdmissionRevision')),'difference':string,'changeAmount':ref('Money')},['id','code','visitDate','version','status',*contact_fields,*counts,'quote'])
schemas['GroupPage']=obj({'items':array(ref('Group')),'page':{'type':'integer'},'size':{'type':'integer'},'total':{'type':'integer','format':'int64'}})
schemas['EditReservation']=obj({'version':string,'request':ref('ReservationRequest')})
endpoint('/cash-sessions/current','get','currentCashSession',ref('CurrentCashSession'))
paths['/cash-sessions/current']['get']['description']='ADMIN y STAFF; openingFloat solo se devuelve a ADMIN. Una jornada anterior abierta bloquea cobros nuevos.'
endpoint('/cash-sessions','post','openCashSession',ref('CurrentCashSession'),'OpenCashSession')
paths['/cash-sessions']['post']['description']='Solo ADMIN. Abre hoy, una sola jornada por fecha y una abierta a la vez. Fondo no negativo.'
for path in ['/reservations','/admissions']:
    endpoint(path,'get','list'+path[1:].capitalize(),ref('GroupPage'))
    paths[path]['get']['parameters']=[{'in':'query','name':'date','required':True,'schema':date},{'in':'query','name':'search','schema':{'type':'string','maxLength':120}},{'in':'query','name':'page','schema':{'type':'integer','minimum':0,'default':0}},{'in':'query','name':'size','schema':{'type':'integer','minimum':1,'maximum':100,'default':20}}]
    endpoint(path+'/{id}','get','get'+path[1:].capitalize(),ref('Group'))
    paths[path+'/{id}']['get']['parameters']=[{'in':'path','name':'id','required':True,'schema':{'type':'integer','format':'int64'}}]
endpoint('/reservations','post','createInternalReservation',ref('Confirmation'),'ReservationRequest')
endpoint('/reservations/{id}/quotes','post','quoteReservationEdit',ref('ReservationQuote'),'EditReservation')
endpoint('/reservations/{id}','patch','editReservation',ref('Group'),'EditReservation')
endpoint('/admissions/quotes','post','quoteAdmission',ref('ReservationQuote'),'AdmissionRequest')
endpoint('/admissions','post','createAdmission',ref('Group'),'AdmissionRequest')
endpoint('/admissions/{id}','patch','updateAdmissionContact',ref('Group'),'AdmissionContact')
endpoint('/admissions/{id}/correction-quotes','post','quoteAdmissionCorrection',obj({'quote':ref('ReservationQuote'),'difference':string}),'AdmissionCorrection')
endpoint('/admissions/{id}/corrections','post','correctAdmission',ref('Group'),'AdmissionCorrection')
for path in ['/reservations/{id}/quotes','/reservations/{id}','/admissions/{id}','/admissions/{id}/correction-quotes','/admissions/{id}/corrections']:
    for method,op in paths[path].items():
        if method!='get':op.setdefault('parameters',[]).append({'in':'path','name':'id','required':True,'schema':{'type':'integer','format':'int64'}})
        op['responses']['404']={'description':'No encontrado','content':{'application/json':{'schema':ref('Error')}}}
for path in ['/reservations','/admissions','/admissions/{id}/corrections']:
    op=paths[path]['post'];op['responses']['201']=op['responses'].pop('200');op['parameters'].append({'in':'header','name':'Idempotency-Key','required':True,'schema':{'type':'string','format':'uuid'},'description':'UUID v4; conservar clave y cuerpo ante respuesta incierta.'})
paths['/admissions']['post']['description']='ADMIN o STAFF. Jornada abierta de hoy. Reserva: reservationId y reservationVersion, cantidades idénticas; sin reserva: rateVersion cotizada y contacto opcional. Total y vuelto en servidor, ingreso/pago/estado/auditoría/idempotencia atómicos. Total cero no crea Payment.'
paths['/admissions/{id}']['patch']['description']='ADMIN o STAFF. Reemplaza contacto y nota con versión; no modifica cantidades ni movimientos.'
for path in ['/admissions/{id}/corrections','/admissions/{id}/correction-quotes']:paths[path]['post']['description']='Solo ADMIN. Motivo y versión obligatorios. Tarifas originales; aumento crea ingreso adicional, reducción crea devolución sin superar neto. Se usa jornada actual abierta y fecha original de asistencia. No altera cierres existentes.'
nullable = lambda name: {'allOf':[ref(name)],'nullable':True}
identifier = {'type':'string','pattern':'^[1-9][0-9]*$'}
method = {'type':'string','enum':['CASH','SINPE','CARD']}
reason = {'type':'string','minLength':1,'maxLength':1000}
schemas['SignedMoney']={'type':'string','pattern':r'^-?[0-9]+\.[0-9]{2}$'}
schemas['ExpenseRequest']=obj({'amount':ref('Money'),'method':method,'category':{'type':'string','minLength':1,'maxLength':120},'description':reason})
schemas['RefundRequest']=obj({'admissionId':identifier,'amount':ref('Money'),'method':method,'reason':reason})
schemas['CompensationRequest']=obj({'sourceType':{'type':'string','enum':['EXPENSE','REFUND','ADJUSTMENT']},'sourceId':identifier,'reason':reason})
schemas['CloseRequest']=obj({'version':string,'countedCash':ref('Money'),'note':{'type':'string','maxLength':1000,'nullable':True},'locale':{'type':'string','enum':['es','en','pt']}},['version','countedCash','locale'])
schemas['FinancialMovement']=obj({'id':identifier,'cashSessionId':identifier,'amount':ref('SignedMoney'),'method':method,'actorId':identifier,'actorName':string,'createdAt':{'type':'string','format':'date-time'},'admissionId':{'type':'string','nullable':True},'code':string,'visitDate':date,'category':string,'description':string,'reason':string,'kind':string,'expenseId':{'type':'string','nullable':True},'refundId':{'type':'string','nullable':True},'originalAdjustmentId':{'type':'string','nullable':True},'revisionId':{'type':'string','nullable':True},'paymentId':{'type':'string','nullable':True},**price_fields},['id','cashSessionId','amount','method','actorId','createdAt'])
schemas['FinancialTotals']=obj({**{k:ref('SignedMoney') for k in ['payments','expenses','refunds','adjustments','additionalPayments','compensations','operatingResult','expectedCash']},**{k:{'type':'integer','format':'int64','minimum':0} for k in counts}})
schemas['FinancialMethod']=obj({'method':method,**{k:ref('SignedMoney') for k in ['payments','expenses','refunds','adjustments','net']}})
schemas['Attendance']=obj({'id':identifier,'code':string,'name':{'type':'string','nullable':True},'visitDate':date,**counts,**price_fields,'reservationId':{'type':'string','nullable':True}})
schemas['FinancialSnapshot']=obj({'schemaVersion':{'type':'integer'},'cashSessionId':identifier,'businessDate':date,'timezone':string,'currency':{'type':'string','enum':['CRC']},'openingFloat':ref('Money'),'totals':ref('FinancialTotals'),'methods':array(ref('FinancialMethod')),**{k:array(ref('FinancialMovement')) for k in ['payments','expenses','refunds','adjustments']},'attendance':array(ref('Attendance')),'closureId':identifier,'closedAt':{'type':'string','format':'date-time'},'locale':{'type':'string','enum':['es','en','pt']},'actorId':identifier,'actorName':string,'recipient':string,'countedCash':ref('Money'),'difference':ref('SignedMoney'),'note':{'type':'string','nullable':True},'businessName':string,'legacy':{'type':'boolean'}},['schemaVersion','cashSessionId','businessDate','timezone','currency','openingFloat','totals','methods','payments','expenses','refunds','adjustments','attendance'])
schemas['FinanceSession']=obj({'id':identifier,'businessDate':date,'status':{'type':'string','enum':['OPEN','CLOSED']},'openingFloat':ref('Money'),'version':string})
schemas['FinanceSummary']=obj({'serverDate':date,'session':nullable('FinanceSession'),'snapshot':nullable('FinancialSnapshot'),'closureId':{'type':'string','nullable':True}})
schemas['Closure']=obj({'id':identifier,'cashSessionId':identifier,'countedCash':ref('Money'),'expectedCash':ref('SignedMoney'),'difference':ref('SignedMoney'),'operatingResult':ref('SignedMoney'),'note':{'type':'string','nullable':True},'recipient':string,'locale':string,'actorId':identifier,'createdAt':{'type':'string','format':'date-time'},'snapshot':ref('FinancialSnapshot'),'email':{'type':'object','nullable':True,'properties':{'id':identifier,'status':{'type':'string','enum':['PENDING','PROCESSING','SENT','FAILED']},'attempts':{'type':'integer'},'lastErrorCode':{'type':'string','nullable':True}}}})
endpoint('/finance/summary','get','getFinanceSummary',ref('FinanceSummary'))
paths['/finance/summary']['get']['parameters']=[{'in':'query','name':'date','required':True,'schema':date}]
endpoint('/finance/admissions/{id}/refundable','get','getRefundableBalance',obj({'id':identifier,'code':string,'visitDate':date,'available':ref('Money')}))
endpoint('/expenses','post','createExpense',ref('FinancialMovement'),'ExpenseRequest')
endpoint('/refunds','post','createRefund',ref('FinancialMovement'),'RefundRequest')
endpoint('/adjustments/quotes','post','quoteCompensation',obj({'source':ref('FinancialMovement'),'amount':ref('SignedMoney'),'method':method}),'CompensationRequest')
endpoint('/adjustments','post','createCompensation',ref('FinancialMovement'),'CompensationRequest')
endpoint('/cash-sessions/{id}/close','post','closeCashSession',ref('Closure'),'CloseRequest')
endpoint('/closures/{id}','get','getClosure',ref('Closure'))
endpoint('/closures/{id}/report','get','downloadClosureReport',string)
endpoint('/closures/{id}/email','post','retryClosureEmail',ref('Closure'))
finance_paths=['/finance/summary','/finance/admissions/{id}/refundable','/expenses','/refunds','/adjustments/quotes','/adjustments','/cash-sessions/{id}/close','/closures/{id}','/closures/{id}/report','/closures/{id}/email']
for path in finance_paths:
    for verb,op in paths[path].items():
        op['description']='Solo ADMIN. Importes CRC exactos; movimientos inmutables y compensaciones vinculadas. Los movimientos nuevos requieren jornada actual abierta.'
        op['responses']['404']={'description':'No encontrado','content':{'application/json':{'schema':ref('Error')}}}
        if '{id}' in path:op.setdefault('parameters',[]).append({'in':'path','name':'id','required':True,'schema':{'type':'integer','format':'int64','minimum':1}})
for path in ['/expenses','/refunds','/adjustments','/cash-sessions/{id}/close']:
    op=paths[path]['post'];op['responses']['201']=op['responses'].pop('200');op['parameters'].append({'in':'header','name':'Idempotency-Key','required':True,'schema':{'type':'string','format':'uuid'},'description':'Conservar clave y cuerpo ante respuesta incierta. Cambiar cuerpo con la misma clave devuelve 409.'})
paths['/finance/summary']['get']['description']='Solo ADMIN. Devuelve instantánea completa de una jornada, sin paginación. Jornada cerrada: datos guardados inmutables. Sin jornada: session, snapshot y closureId nulos. El seed antiguo se identifica con legacy y no inventa nombres históricos.'
paths['/refunds']['post']['description']+=' Importe positivo sin superar saldo neto, incluso concurrentemente; no modifica asistencia.'
paths['/adjustments']['post']['description']+=' Importe y método calculados desde el origen. Compensa el saldo no compensado del gasto/devolución o revierte un ajuste una sola vez. No admite ingresos genéricos.'
paths['/cash-sessions/{id}/close']['post']['description']='Solo ADMIN. version de FinanceSummary; contado no negativo y nota obligatoria si hay diferencia. Puede cerrar la jornada anterior abierta; no permite reabrir. Congela movimientos, asistencia, tarifas, responsable, idioma y destinatario. Cierre y correo pendiente atómicos. Si ya existe, devuelve el mismo cierre.'
paths['/closures/{id}/email']['post']['description']='Solo ADMIN. Cambia FAILED a PENDING sin duplicar evento, conserva Message-ID, idioma y destinatario. PENDING, PROCESSING y SENT no se reenvían. SMTP deshabilitado mantiene el correo en cola.'
op=paths['/closures/{id}/report']['get'];op['parameters'].append({'in':'query','name':'format','required':True,'schema':{'type':'string','enum':['pdf','xlsx']}})
op['responses']['200']={'description':'Reporte generado únicamente desde la instantánea. XLSX: Summary, Payments, Expenses, Refunds, Adjustments, Attendance; importes como texto decimal exacto. Etiquetas en idioma guardado.','headers':{'Content-Disposition':{'schema':string},'Cache-Control':{'schema':string}},'content':{mime:{'schema':{'type':'string','format':'binary'}} for mime in ['application/pdf','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet']}}
op['responses']['503']={'description':'Generación fallida; el cierre permanece confirmado','content':{'application/json':{'schema':ref('Error')}}}

schemas['TaskWrite']=obj({'taskDate':date,'title':{'type':'string','minLength':1,'maxLength':120},'description':{'type':'string','minLength':1,'maxLength':2000},'status':{'type':'string','enum':['PENDING','COMPLETED']},'version':{'type':'string','nullable':True}},['taskDate','title','description','status'])
schemas['TaskDelete']=obj({'version':string})
schemas['Task']=obj({'id':identifier,'taskDate':date,'title':string,'description':string,'status':{'type':'string','enum':['PENDING','COMPLETED']},'version':string,'authorId':identifier,'authorName':string,'updatedBy':identifier,'updatedByName':string,'createdAt':{'type':'string','format':'date-time'},'updatedAt':{'type':'string','format':'date-time'}})
schemas['Calendar']=obj({'month':string,'serverDate':date,'days':array(obj({'date':date,'reservations':{'type':'integer','format':'int64'},'tasks':{'type':'integer','format':'int64'},'pendingTasks':{'type':'integer','format':'int64'}}))})
def page_schema(item):return obj({'items':array(ref(item)),'page':{'type':'integer'},'size':{'type':'integer'},'total':{'type':'integer','format':'int64'}})
page_parameters=[{'in':'query','name':'page','schema':{'type':'integer','minimum':0,'default':0}},{'in':'query','name':'size','schema':{'type':'integer','minimum':1,'maximum':100,'default':20}}]
endpoint('/calendar','get','getCalendar',ref('Calendar'))
paths['/calendar']['get']['parameters']=[{'in':'query','name':'month','schema':{'type':'string','pattern':'^[0-9]{4}-[0-9]{2}$'},'description':'Mes gregoriano entre 0001 y 9999. Omitir para mes actual según servidor.'}]
paths['/calendar']['get']['description']='ADMIN y STAFF. Marcador de reservas CONFIRMED y todas las tareas, incluidas COMPLETED. Para detalle del día usar /reservations?date y /tasks?date, paginados.'
endpoint('/tasks','get','listTasks',page_schema('Task'))
paths['/tasks']['get']['parameters']=[{'in':'query','name':'date','required':True,'schema':date},*page_parameters]
endpoint('/tasks/{id}','get','getTask',ref('Task'))
endpoint('/tasks','post','createTask',ref('Task'),'TaskWrite')
paths['/tasks']['post']['responses']['201']=paths['/tasks']['post']['responses'].pop('200')
endpoint('/tasks/{id}','patch','updateTask',ref('Task'),'TaskWrite')
endpoint('/tasks/{id}','delete','deleteTask',obj({'id':identifier,'status':{'type':'string','enum':['DELETED']}}),'TaskDelete')
for path in ['/tasks','/tasks/{id}']:
    for verb,op in paths[path].items():
        op['description']='ADMIN y STAFF gestionan tareas compartidas. Fecha SQL válida; textos recortados no vacíos. PATCH y DELETE exigen version Base64 vigente; conflicto 409 conserva la otra edición. DELETE requiere confirmación en UI y queda auditado. Completar mantiene la tarea visible. Sin asignación ni alarmas.'
        if '{id}' in path:op.setdefault('parameters',[]).append({'in':'path','name':'id','required':True,'schema':{'type':'integer','format':'int64'}})
        if verb!='get':op.setdefault('parameters',[]).append({'in':'header','name':'Idempotency-Key','required':True,'schema':{'type':'string','format':'uuid'},'description':'UUID v4, conservar cuerpo y clave al reintentar. Reproducción aislada por usuario y operación durante al menos 49 horas.'})
        op['responses']['404']={'description':'Tarea no encontrada','content':{'application/json':{'schema':ref('Error')}}}
schemas['ActivityGroup']=obj({'groupKey':string,'occurredAt':{'type':'string','format':'date-time'},'actorId':{'type':'string','nullable':True},'actorName':{'type':'string','nullable':True},'entryCount':{'type':'integer','format':'int64'},'type':string})
schemas['ActivityEntry']=obj({'id':string,'groupKey':string,'occurredAt':{'type':'string','format':'date-time'},'actorId':{'type':'string','nullable':True},'actorName':{'type':'string','nullable':True},'entity':{'type':'string','nullable':True},'entityId':{'type':'string','nullable':True},'type':string,'outcome':string,'origin':{'type':'string','enum':['APPLICATION','EXTERNAL']},'source':{'type':'string','enum':['DML','EVENT']}})
endpoint('/activity','get','listActivity',page_schema('ActivityGroup'))
paths['/activity']['get']['parameters']=[{'in':'query','name':key,'required':True,'schema':date} for key in ['from','to']]+[{'in':'query','name':'actorId','schema':{'type':'integer','format':'int64','minimum':1}},{'in':'query','name':'type','schema':{'type':'string','maxLength':80}},{'in':'query','name':'entity','schema':{'type':'string','maxLength':80}},*page_parameters]
endpoint('/activity/options','get','getActivityOptions',obj({'actors':array(obj({'id':identifier,'name':string})),'types':array(string),'entities':array(string)}))
endpoint('/activity/{group}/entries','get','getActivityEntries',page_schema('ActivityEntry'))
paths['/activity/{group}/entries']['get']['parameters']=[{'in':'path','name':'group','required':True,'schema':string},*page_parameters]
paths['/activity/{group}/entries']['get']['responses']['404']={'description':'Grupo no encontrado','content':{'application/json':{'schema':ref('Error')}}}
for path in ['/activity','/activity/options','/activity/{group}/entries']:
    paths[path]['get']['description']='Solo ADMIN, lectura. Fechas inclusivas de Costa Rica convertidas a UTC. Agrupa filas filtradas por requestId; sin solicitud usa A-id/E-id independiente. El detalle paginado muestra todas las filas relacionadas, incluso fuera del filtro inicial. No devuelve contactos, credenciales, claves, notas, títulos/descripciones privados ni JSON antes/después. Actor nulo significa desconocido/proceso; no inventa identidad. Nombres de usuario actuales. No existen mutaciones de auditoría.'

document={'openapi':'3.0.3','info':{'title':'Tourism Business API','version':'0.6.0','description':'Acceso y configuración. Identificadores, importes y tasas se envían como cadenas. Sesión HttpOnly, SameSite=Lax, Secure en producción; CSRF también en login. Configuración, tarifas y tasas requieren ADMIN. Incluye disponibilidad, cotización, confirmación pública idempotente y transiciones internas de reserva. Incluye apertura, ingresos, correcciones, finanzas, cierres inmutables y reportes PDF/XLSX.'},'servers':[{'url':'/api/v1'}],'paths':paths,'components':{'schemas':schemas,'securitySchemes':{'Session':{'type':'apiKey','in':'cookie','name':'JSESSIONID'}}}}
(root / 'docs/openapi.yaml').write_text(json.dumps(document,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
