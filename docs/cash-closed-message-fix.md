# Corrección del aviso de caja cerrada

28 de septiembre de 2026.

La captura reportada mostraba la jornada del día CLOSED, pero Cobros indicaba que ADMIN debía cerrar una jornada anterior. La plantilla usaba el mismo mensaje para cualquier canPay=false.

Se diferencia CLOSED de una jornada OPEN anterior en español, inglés y portugués. Finanzas también explica que no se puede reabrir ni sustituir una jornada cerrada del mismo día. Consultar históricos sigue disponible y no cambia la jornada operativa. Se conservan las restricciones de cobro y el cierre guardado, sin cambios de base de datos ni API.

Verificación: cinco archivos y 34 pruebas del panel aprobados, incluidos tres casos de regresión por idioma que distinguen caja cerrada de caja anterior abierta y comprueban cobro deshabilitado y filtro histórico disponible. Build Angular correcto, 439,01 kB iniciales. Fuente actualizada en la copia temporal NTFS y dist copiado al repositorio. No se afirma comprobación visual de la sesión del usuario.

El 401 de /auth/me puede ser la consulta inicial sin sesión: App.ngOnInit lo maneja mostrando acceso. La captura no acredita que ese 401 ocurriera durante el cierre. Los mensajes de conexión del receptor y ruta de archivos no se atribuyen a la API sin evidencia adicional; no se modificaron extensiones ni protecciones del navegador.
