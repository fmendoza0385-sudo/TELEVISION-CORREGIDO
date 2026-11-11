# DOCUMENTO MAESTRO DE DESARROLLO

## Sistema de suscripción, renovación y bloqueo por vencimiento

### LUKKA Televisión --- Android TV y Smartphone

**Versión:** 1.2\
**Fecha:** 11 de noviembre de 2026\
**Estado:** Especificación ampliada con inventario de 500 usuarios y banco JSON temporal; implementación Android pendiente\
**Organización / licencia:** GRM CONEXIÓN SAS\
**Plataformas objetivo:** Android TV y Android Smartphone

------------------------------------------------------------------------

## 1. Propósito del documento

Este documento define los requisitos funcionales, visuales, técnicos y
de entrega para incorporar a **LUKKA Televisión** un sistema de
activación mensual y una pantalla de bloqueo por suscripción vencida.

En esta etapa se establece únicamente la especificación del proyecto.
**No se implementa código, no se modifica un APK y no se crea todavía un
repositorio ni un ZIP de código fuente.** El documento servirá como guía
para preparar posteriormente el proyecto Android completo, empaquetarlo
y desarrollarlo mediante GitHub.

El objetivo principal es que el usuario final pueda encender el
dispositivo y ver televisión sin tener que comprender configuraciones
técnicas. Cuando la suscripción no esté vigente, la aplicación debe
mostrar una pantalla clara, amable y fácil de manejar, especialmente
para adultos mayores.

## 2. Documentos y requisitos de referencia

La especificación conserva los requisitos del PRD de LUKKA Televisión
suministrado por el usuario:

-   Aplicación IPTV para Android TV y Smartphone.
-   Experiencia de uso similar a un decodificador tradicional.
-   Identidad visual azul metalizado, blanco y negro.
-   Inicio directo al último canal visto o al canal predeterminado.
-   Activación mediante ID de usuario y código mensual.
-   Planes de 50, 80 y 120 canales.
-   Tres matrices de códigos mensuales de hasta 36 meses por usuario.
-   Vinculación de cada subcuenta con su dispositivo.
-   Corte por falta de renovación el día 5 de cada mes.
-   Subcuentas diferenciadas para TV principal, TV adicional y móvil.

Este documento desarrolla específicamente el módulo de vencimiento y
bloqueo, y define cómo debe integrarse sin romper el resto de la
aplicación.

## 3. Objetivos del módulo

1.  Detectar si la suscripción mensual está vigente.
2.  Bloquear la reproducción cuando el código haya vencido o no exista
    un código válido para el ciclo correspondiente.
3.  Aplicar la regla de corte mensual del día 5.
4.  Permitir ingresar un nuevo código sin reinstalar la aplicación.
5.  Reactivar automáticamente el servicio después de validar
    correctamente el código.
6.  Mantener la vinculación entre ID de usuario, subcuenta y
    dispositivo.
7.  Evitar que el usuario final pueda omitir el bloqueo mediante
    navegación, botones del control remoto o cierre de una pantalla
    secundaria.
8.  Ofrecer una experiencia accesible, coherente y consistente en
    Android TV y Smartphone.

## 4. Alcance

### 4.1 Incluido

-   Pantalla de servicio suspendido.
-   Pantalla o formulario de activación y renovación.
-   Validación de ID de usuario y código mensual.
-   Control de vigencia y fecha de corte.
-   Integración con el reproductor y el flujo de arranque.
-   Comprobación de vinculación del dispositivo.
-   Manejo de estados de activación, vencimiento, error y renovación.
-   Persistencia segura de la información necesaria.
-   Compatibilidad con navegación por control remoto y pantalla táctil.
-   Pruebas funcionales y compilación Android mediante GitHub Actions.

### 4.2 Fuera del alcance de esta etapa

-   Desarrollo de un panel administrativo web.
-   Creación de una plataforma de pagos en línea.
-   Generación automática de códigos desde WhatsApp.
-   Cambios en la fuente o titularidad de las listas IPTV.
-   Reestructuración completa del reproductor, EPG u OSD, salvo los
    ajustes imprescindibles para integrar el bloqueo.
-   Publicación en Google Play.
-   Implementación del código fuente en esta fase documental.

Cualquier ampliación de alcance deberá aprobarse antes de incorporarse
al proyecto.

## 5. Experiencia de usuario y diseño visual

### 5.1 Identidad visual

La interfaz debe respetar la identidad definida para LUKKA Televisión:

-   **Color principal:** azul metalizado.
-   **Color secundario:** blanco.
-   **Fondo y acentos:** negro.
-   Contraste alto y tipografía legible.
-   Botones grandes, separados y fáciles de seleccionar.
-   Iconografía sencilla y reconocible.
-   Evitar textos técnicos, ventanas pequeñas y menús innecesarios.

No se deben inventar ni sustituir el logotipo oficial ni los datos de
contacto. Se utilizarán los recursos oficiales que se incluyan en el ZIP
del proyecto.

### 5.2 Mensaje de suspensión

Texto base aprobado para la pantalla:

**LUKKA TELEVISIÓN**

**SERVICIO SUSPENDIDO**

Tu suscripción se encuentra vencida.

Para continuar disfrutando de tu televisión, necesitas renovar tu código
de activación mensual.

**¿CÓMO RECUPERAR TU SERVICIO?**

Comunícate con tu proveedor y solicita tu nuevo código mensual.

Botón principal: **ACTIVAR SERVICIO**

Pie de pantalla: **LUKKA TELEVISIÓN · GRM CONEXIÓN SAS**

El texto debe poder ajustarse desde recursos de la aplicación, sin tener
que modificar la lógica de validación.

### 5.3 Android TV

-   La pantalla debe ocupar el área completa.
-   Debe ocultar las barras del sistema cuando la plataforma lo permita.
-   El botón **ACTIVAR SERVICIO** tendrá foco inicial.
-   El usuario podrá navegar con las flechas del control remoto y
    confirmar con OK/Enter.
-   Los botones y campos tendrán áreas de selección amplias.
-   La tecla Atrás no podrá regresar al reproductor mientras exista un
    bloqueo vigente.
-   El teclado en pantalla debe permitir ingresar el ID y el código.
-   No se exigirá ratón ni pantalla táctil.

### 5.4 Smartphone

-   Diseño adaptable a distintas resoluciones y densidades.
-   Diseño vertical como presentación principal y adaptación horizontal
    cuando corresponda.
-   Campos cómodos para escribir y botones amplios.
-   Teclado apropiado para el formato del código.
-   Mensajes de error visibles, concretos y fáciles de entender.
-   No mostrar elementos exclusivos del control remoto.

## 6. Reglas de negocio de la suscripción

### 6.1 Identificación de cuenta y dispositivo

Cada activación se asociará como mínimo a:

-   ID de usuario o subcuenta.
-   Identificador del dispositivo registrado.
-   Plan autorizado.
-   Ciclo mensual al que corresponde el código.
-   Estado de activación.
-   Fecha de inicio y fecha de vencimiento.
-   Referencia de la validación o licencia, cuando exista servidor de
    licencias.

Ejemplos de identificadores definidos en el PRD:

-   `USER-001`: TV principal.
-   `USER-001-TV2`: segundo televisor.
-   `USER-001-MOVIL`: dispositivo móvil.

El ID de la subcuenta no debe confundirse con el identificador físico
del dispositivo.

### 6.2 Día de corte

La regla de producto establece que el día 5 de cada mes, si no se
introduce un nuevo código válido, la aplicación debe suspender el
servicio.

La zona horaria de negocio será `America/Bogota`.

**Regla operativa propuesta para la implementación:** evaluar el
vencimiento desde las 00:00 del día 5, hora de Colombia, salvo que el
administrador del servicio defina otra hora antes de iniciar el
desarrollo. No se debe conceder un período de gracia implícito.

La aplicación debe comparar fechas de forma consistente y no depender
únicamente del reloj local del teléfono o TV Box.

### 6.3 Estados de suscripción

Se deben manejar, como mínimo, los siguientes estados:

  -----------------------------------------------------------------------------
  Estado                        Significado             Comportamiento
  ----------------------------- ----------------------- -----------------------
  `NO_ACTIVADA`                 No existe activación    Mostrar activación
                                válida en el            
                                dispositivo             

  `ACTIVA`                      Código válido para el   Permitir reproducción
                                ciclo actual            

  `POR_VENCER`                  Se aproxima el          Puede mostrarse aviso
                                siguiente corte         no intrusivo

  `VENCIDA`                     Terminó la vigencia del Bloquear reproducción
                                código                  

  `CODIGO_INVALIDO`             El código no coincide o Mantener bloqueo y
                                no corresponde al ciclo explicar el error

  `DISPOSITIVO_NO_AUTORIZADO`   La licencia está        Mantener bloqueo y
                                vinculada a otro equipo mostrar instrucciones

  `VALIDACION_PENDIENTE`        No se pudo completar la Aplicar política segura
                                validación              de validación

  `ERROR_SERVIDOR`              El servicio de          Mostrar mensaje de
                                licencias no está       conexión y permitir
                                disponible              reintento
  -----------------------------------------------------------------------------

Los estados transitorios de error no deben convertirse automáticamente
en una activación válida.

### 6.4 Tabla de decisión

  -----------------------------------------------------------------------
  Condición                           Resultado esperado
  ----------------------------------- -----------------------------------
  Código vigente y dispositivo        Acceso a televisión
  autorizado                          

  Código vencido                      Pantalla de bloqueo

  Día 5 o posterior y no hay código   Pantalla de bloqueo
  del nuevo ciclo                     

  Código incorrecto                   Mantener pantalla de activación

  Código válido y dispositivo         Registrar renovación y habilitar
  autorizado                          televisión

  Código válido asociado a otro       Rechazar activación y conservar
  dispositivo                         bloqueo

  Fecha del equipo modificada         No conceder acceso por esa sola
                                      modificación

  Servidor no disponible              No inventar una validación;
                                      informar y reintentar según
                                      política
  -----------------------------------------------------------------------

## 7. Flujo de arranque e integración con el reproductor

El módulo debe integrarse en el flujo existente de arranque de LUKKA
Televisión.

Secuencia requerida:

1.  Iniciar la aplicación y mostrar el splash oficial.
2.  Cargar los componentes esenciales.
3.  Consultar el estado local de la suscripción.
4.  Cuando exista conectividad, comprobar la vigencia con el mecanismo
    de validación autorizado.
5.  Evaluar la vinculación del dispositivo.
6.  Si la suscripción está activa, continuar el flujo de carga y
    reproducción directa definido por el PRD.
7.  Si no está activada o está vencida, abrir la pantalla de activación
    o bloqueo.
8.  Al validar la renovación, actualizar el estado de licencia y
    continuar con la carga del último canal visto o canal
    predeterminado.

El reproductor no debe comenzar a reproducir audio o video detrás de la
pantalla de bloqueo. La comprobación debe realizarse antes de habilitar
la reproducción.

La pantalla de bloqueo debe permanecer como estado principal de la
aplicación mientras la licencia no sea válida. No basta con abrir un
diálogo encima del reproductor.

## 8. Arquitectura lógica propuesta

La implementación debe separar la interfaz visual, las reglas de negocio
y el acceso a los datos.

### 8.1 Componentes

  -----------------------------------------------------------------------
  Componente                          Responsabilidad
  ----------------------------------- -----------------------------------
  `SubscriptionManager`               Coordinar consulta y estado de
                                      suscripción

  `SubscriptionValidator`             Evaluar código, ciclo, vigencia y
                                      respuesta de validación

  `ActivationRepository`              Obtener y guardar datos de
                                      activación

  `DeviceBindingManager`              Gestionar la asociación con el
                                      dispositivo

  `SubscriptionPolicy`                Aplicar las reglas de fecha de
                                      corte

  `BlockingActivity` o equivalente    Mostrar la pantalla de suspensión

  `ActivationActivity` o equivalente  Capturar ID y código

  `MainActivity`                      Controlar el flujo principal y
                                      evitar iniciar reproducción sin
                                      licencia

  `LicenseApi` (si se usa servidor)   Comunicar la aplicación con el
                                      servicio de licencias
  -----------------------------------------------------------------------

Los nombres son una propuesta de organización. Antes de programar se
deben adaptar a la arquitectura y lenguaje reales del proyecto Android
que se entregue.

### 8.2 Responsabilidades de la pantalla de bloqueo

La pantalla de bloqueo debe:

-   Mostrar el motivo general de suspensión.
-   Presentar el botón de activación.
-   Permitir volver al formulario de activación, no al reproductor.
-   Mantener el bloqueo si el usuario pulsa Atrás.
-   Mostrar errores de forma comprensible.
-   Permitir reintentar una validación.
-   No contener funciones administrativas.
-   No exponer códigos internos, claves maestras ni información
    sensible.

### 8.3 Responsabilidades del validador

El validador debe comprobar:

1.  Que el ID exista.
2.  Que el código corresponda a ese ID.
3.  Que el código pertenezca al ciclo mensual esperado.
4.  Que el plan asociado esté autorizado.
5.  Que el dispositivo esté autorizado.
6.  Que la licencia no esté revocada.
7.  Que las fechas y el estado de vigencia sean coherentes.

La interfaz no debe decidir por sí sola que un código es válido. Debe
recibir el resultado del componente de validación.

## 9. Seguridad y protección de licencias

### 9.1 Principios obligatorios

-   No guardar contraseñas, claves maestras ni secretos de servidor en
    texto plano.
-   No incluir una clave privada de firma dentro del APK.
-   No considerar el cifrado local como prueba suficiente de que una
    licencia sigue vigente.
-   No confiar exclusivamente en la fecha y hora configuradas por el
    usuario.
-   No permitir que una respuesta de error se interprete como una
    licencia válida.
-   No mostrar información que permita reconstruir el banco de códigos.
-   No registrar códigos completos en logs de producción.

### 9.2 Validación recomendada

Para una protección robusta se recomienda un servicio de licencias
remoto que valide el código y devuelva una respuesta firmada o
verificable por la aplicación.

La respuesta de validación debería incluir, como mínimo:

-   Resultado: válido o rechazado.
-   ID de usuario o subcuenta.
-   Identificador de licencia.
-   Plan autorizado.
-   Ciclo mensual.
-   Inicio y fin de vigencia.
-   Identificador del dispositivo autorizado.
-   Fecha/hora de validación del servidor.
-   Versión del esquema de respuesta.

La aplicación debe verificar la autenticidad de la respuesta antes de
habilitar la reproducción.

### 9.3 Funcionamiento sin Internet

El PRD describe una aplicación IPTV que utiliza conectividad para
reproducir televisión. La política exacta de validación de licencias
cuando no exista Internet debe definirse antes de programar.

Recomendación:

-   Conservar localmente la última licencia válida, protegida y con
    vencimiento verificable.
-   No extender la vigencia por falta de conexión.
-   No permitir que una licencia vencida se reactive sin validación.
-   Si no se puede confirmar el estado y no existe una licencia local
    vigente y verificable, mostrar un mensaje de conexión y un botón de
    reintento.
-   La política de tolerancia de desconexión, si se aprueba, debe ser
    explícita y limitada; nunca debe ser una renovación automática.

## 10. Banco de códigos y planes

Se conserva la estructura conceptual establecida en el PRD:

-   `PLAN_50`: lista `m3u_plan_50.m3u`.
-   `PLAN_80`: lista `m3u_plan_80.m3u`.
-   `PLAN_120`: lista `m3u_plan_120.m3u`.

Cada usuario podrá disponer de hasta 36 códigos mensuales por plan,
según la matriz definida para su cuenta.

El cambio de plan se determina por el código autorizado del ciclo. La
aplicación debe seleccionar la lista correspondiente sin solicitar al
usuario que configure manualmente la M3U.

### 10.1 Consideración sobre el JSON local

El PRD original contempla una base JSON cifrada con AES-256, hasta 500
usuarios y 54.000 registros globales. Esa estructura puede conservarse
como formato de datos o respaldo, pero no debe considerarse por sí sola
una autoridad invulnerable de licencias cuando el archivo y la
aplicación están bajo control del usuario del dispositivo.

Si se mantiene JSON local:

-   Definir y documentar el esquema y su versión.
-   Validar campos y tipos al leerlo.
-   Manejar archivos dañados o incompletos sin bloquear la aplicación
    con un cierre inesperado.
-   No incluir claves de descifrado recuperables fácilmente dentro del
    APK.
-   Mantener una fuente autorizada para emitir, renovar o revocar
    códigos.

## 11. Estructura propuesta del ZIP del proyecto

El ZIP que se preparará para GitHub debe contener el proyecto Android
completo y abrirse directamente en Android Studio.

Estructura orientativa:

``` text
LUKKA_Television_Android/
├── README.md
├── DOCUMENTO_MAESTRO.md
├── LICENSE
├── .gitignore
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── app/
│   ├── build.gradle
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/
│           │   └── .../
│           │       ├── MainActivity
│           │       ├── subscription/
│           │       │   ├── SubscriptionManager
│           │       │   ├── SubscriptionValidator
│           │       │   ├── SubscriptionPolicy
│           │       │   └── DeviceBindingManager
│           │       ├── activation/
│           │       │   └── ActivationActivity
│           │       ├── blocking/
│           │       │   └── BlockingActivity
│           │       └── data/
│           │           └── ActivationRepository
│           ├── res/
│           │   ├── drawable/
│           │   ├── mipmap/
│           │   ├── layout/
│           │   ├── values/
│           │   └── xml/
│           └── assets/
│               ├── m3u_plan_50.m3u
│               ├── m3u_plan_80.m3u
│               ├── m3u_plan_120.m3u
│               └── .../
├── .github/
│   └── workflows/
│       └── android-build.yml
└── docs/
    ├── ARQUITECTURA.md
    ├── REGLAS_SUSCRIPCION.md
    ├── FORMATO_LICENCIAS.md
    └── PRUEBAS.md
```

Esta estructura es una guía. No se deben crear carpetas o clases vacías
para aparentar que existe una implementación. Los archivos definitivos
se ajustarán al proyecto Android real, sus paquetes, lenguaje y
dependencias.

### 11.1 Contenido mínimo del ZIP

-   Proyecto Android Studio completo.
-   Gradle Wrapper y archivos de configuración.
-   Código fuente existente del reproductor.
-   Recursos oficiales de LUKKA Televisión.
-   Listas M3U que el propietario tenga autorización para distribuir.
-   Configuración de Android TV y Smartphone.
-   Pruebas disponibles.
-   Este documento maestro.
-   Workflow de GitHub Actions.
-   Instrucciones de compilación.

### 11.2 Archivos que no deben incluirse

-   Contraseñas reales.
-   Tokens privados.
-   Claves de firma privadas.
-   Archivos `.jks` o `.keystore` privados.
-   Credenciales de servicios.
-   Datos personales reales de clientes.
-   Bases de datos de producción.
-   Archivos temporales, cachés y carpetas de compilación innecesarias.

Los secretos se configurarán posteriormente en GitHub Secrets o en el
entorno seguro correspondiente.

## 12. Requisitos de GitHub Actions

El repositorio deberá permitir compilar el proyecto desde GitHub
Actions.

El workflow deberá:

1.  Ejecutarse manualmente y, si se desea, ante cambios en la rama
    principal.
2.  Configurar una versión de JDK compatible con el Gradle del proyecto.
3.  Configurar Android SDK y las plataformas requeridas.
4.  Dar permisos de ejecución a `gradlew` en el entorno Linux.
5.  Ejecutar la compilación de depuración.
6.  Detener el proceso si hay errores.
7.  Publicar el APK generado como artefacto descargable.
8.  Mostrar claramente la versión y el resultado de la compilación.

La versión de Gradle, Android Gradle Plugin, JDK y compile SDK debe
elegirse después de inspeccionar el proyecto real. No se deben
actualizar dependencias de manera indiscriminada.

**Entregable esperado:** APK de prueba instalable en Android TV y
Smartphone compatibles. La firma de producción se gestionará
separadamente y con secretos protegidos.

## 13. Requisitos de compatibilidad y accesibilidad

-   Mantener compatibilidad con los niveles de Android que soporte el
    proyecto original.
-   Declarar correctamente el soporte de Android TV y pantallas
    táctiles.
-   Usar navegación por foco en TV.
-   Mantener contraste suficiente.
-   Evitar tipografías pequeñas.
-   Proporcionar etiquetas accesibles a campos y botones.
-   Evitar que la pantalla se cierre accidentalmente.
-   Evitar que una notificación, diálogo o actividad secundaria permita
    entrar al reproductor.
-   Adaptar el diseño a distintas proporciones de pantalla.

La compatibilidad final se verificará con dispositivos o emuladores
representativos de ambas plataformas.

## 14. Casos de prueba obligatorios

  ------------------------------------------------------------------------
                           N.º Escenario             Resultado esperado
  ---------------------------- --------------------- ---------------------
                             1 Primera instalación   Mostrar activación
                               sin activar           

                             2 Código válido del     Activar el servicio
                               ciclo actual          

                             3 Código incorrecto     Rechazar y conservar
                                                     bloqueo

                             4 ID de usuario         Mostrar error claro
                               inexistente           

                             5 Código de otro        Rechazar
                               usuario               

                             6 Código de un ciclo    Rechazar
                               anterior              

                             7 Suscripción vigente   Permitir reproducción
                               antes del corte       

                             8 Llega el día 5 sin    Mostrar bloqueo
                               renovación válida     

                             9 Se abre la app con    Mostrar bloqueo antes
                               licencia vencida      del video

                            10 Se pulsa CH+ o CH-    No cambiar ni
                               estando bloqueado     reproducir canales

                            11 Se pulsa Atrás        No volver al
                               estando bloqueado     reproductor

                            12 Se fuerza el cierre y Mantener el estado de
                               se vuelve a abrir     bloqueo

                            13 Se introduce un       Reactivar y cargar el
                               código válido de      plan autorizado
                               renovación            

                            14 Código válido         Rechazar y mantener
                               asociado a otro       bloqueo
                               dispositivo           

                            15 No hay conexión con   Mostrar
                               el servidor           error/reintento según
                                                     política

                            16 Se cambia la fecha    No conceder acceso
                               del dispositivo       por el cambio local

                            17 Android TV con        Activación operable
                               control remoto        sin pantalla táctil

                            18 Smartphone en         Interfaz legible y
                               orientación vertical  funcional

                            19 Smartphone en         Interfaz adaptable
                               orientación           
                               horizontal            

                            20 Respuesta de licencia Rechazar validación
                               incompleta o alterada 
  ------------------------------------------------------------------------

## 15. Criterios de aceptación

El módulo se considerará terminado cuando:

-   La pantalla de suspensión se muestre automáticamente ante una
    licencia vencida.
-   El corte del día 5 se aplique conforme a la zona horaria y regla
    aprobadas.
-   La reproducción no se inicie mientras la licencia no sea válida.
-   No sea posible omitir el bloqueo mediante navegación normal.
-   La renovación con código válido reactive el servicio sin reinstalar
    la APK.
-   Se seleccione el plan correcto después de renovar.
-   Se conserve el vínculo de dispositivo.
-   La pantalla funcione con control remoto y pantalla táctil.
-   Los errores de red, datos y validación se manejen sin cierres
    inesperados.
-   GitHub Actions genere el APK de prueba.
-   Se completen los casos de prueba y se documenten los resultados.

## 16. Plan de desarrollo por fases

### Fase 0 --- Preparación del proyecto

-   Recibir y revisar el ZIP Android original.
-   Identificar lenguaje, paquetes, Activities, reproductor y
    dependencias.
-   Confirmar el flujo actual de activación.
-   Verificar que el proyecto compile antes de modificarlo.
-   Preparar la estructura de repositorio y documentación.

**Salida:** diagnóstico técnico y proyecto base reproducible.

### Fase 1 --- Motor de suscripción

-   Implementar estados de licencia.
-   Implementar reglas del ciclo mensual.
-   Implementar comprobación de fecha y dispositivo.
-   Integrar almacenamiento seguro.

**Salida:** motor de suscripción probado sin alterar la interfaz
principal.

### Fase 2 --- Pantalla de bloqueo y activación

-   Crear interfaz de suspensión.
-   Crear o adaptar formulario de activación.
-   Implementar navegación accesible para TV y móvil.
-   Gestionar mensajes y errores.

**Salida:** flujo completo de bloqueo y renovación.

### Fase 3 --- Integración con televisión

-   Integrar el control de licencia al arranque.
-   Impedir reproducción sin licencia.
-   Preservar último canal y zapping cuando el servicio esté activo.
-   Comprobar que OSD y guía no se alteren innecesariamente.

**Salida:** aplicación integrada.

### Fase 4 --- Seguridad y pruebas

-   Probar códigos y ciclos.
-   Probar cambios de fecha.
-   Probar desconexión de Internet.
-   Probar vinculación de dispositivos.
-   Probar controles remotos y pantallas táctiles.

**Salida:** informe de pruebas y correcciones.

### Fase 5 --- GitHub y APK

-   Configurar GitHub Actions.
-   Compilar APK de depuración.
-   Probar instalación en Android TV y Smartphone.
-   Documentar versión, resultado y limitaciones.

**Salida:** repositorio compilable y APK de prueba.

## 17. Decisiones pendientes antes de programar

Los siguientes puntos deben confirmarse o completarse durante la
preparación del ZIP:

1.  **Hora exacta de corte:** se propone 00:00 del día 5 en
    `America/Bogota`.
2.  **Período de gracia:** se propone ninguno, sujeto a confirmación.
3.  **Validación de códigos:** definir si el banco JSON local será
    temporal o si existirá un servidor central de licencias.
4.  **Conectividad:** definir qué hacer si el dispositivo está sin
    Internet y conserva una licencia previamente validada.
5.  **Contacto de soporte:** suministrar el teléfono, WhatsApp o canal
    que aparecerá en la pantalla. No inventar datos.
6.  **Identidad visual:** incluir logotipo oficial y recursos gráficos
    originales.
7.  **Dispositivos de prueba:** indicar modelos o versiones Android
    disponibles, si se conocen.
8.  **Proyecto base:** entregar el ZIP Android completo y funcional
    sobre el que se realizará la integración.

Las decisiones pendientes no deben resolverse mediante supuestos ocultos
en el código. Deben quedar registradas y aprobadas.

## 18. Entregables finales del desarrollo

Al finalizar las fases de desarrollo se espera recibir:

-   Proyecto Android Studio completo y organizado.
-   Código fuente del sistema de suscripción.
-   Pantalla de bloqueo y pantalla de activación.
-   Integración con Android TV y Smartphone.
-   Configuración de GitHub Actions.
-   APK de prueba.
-   Documentación de instalación y compilación.
-   Informe de pruebas.
-   Registro de decisiones de seguridad y operación.

## 19. Instrucción principal para el desarrollador

Desarrollar el sistema de bloqueo por suscripción vencida de LUKKA
Televisión como un módulo integrado, seguro y mantenible. Respetar la
experiencia de decodificador directo, la identidad visual azul
metalizado/blanco/negro y la accesibilidad para adultos mayores.

No sustituir el reproductor ni alterar funciones existentes que no
formen parte del alcance. No habilitar la televisión por una simple
condición visual, por una fecha local manipulable o por una respuesta de
validación no verificada.

La aplicación debe tener un único criterio central para decidir si la
reproducción está permitida. Toda ruta de entrada al reproductor debe
respetar ese criterio.

**La fase actual termina con este documento maestro. La programación
comenzará únicamente después de preparar y revisar el ZIP del proyecto
Android.**

------------------------------------------------------------------------

**FIN DEL DOCUMENTO MAESTRO --- LUKKA TELEVISIÓN**



## 20. Compatibilidad de reproducción: canales con audio y pantalla negra

### 20.1 Problema que se debe corregir

Se ha reportado que algunos canales reproducen el audio, pero muestran la imagen completamente negra. Una causa posible es que el dispositivo Android o el reproductor no pueda decodificar el formato de video, el perfil, el nivel o alguna característica del flujo recibido.

**Aclaración técnica:** MPEG-TS (`.ts`) es un contenedor de transporte, no un códec de video. Un flujo MPEG-TS puede transportar video MPEG-2, H.264/AVC, H.265/HEVC u otros formatos, además de audio. Por eso, no se debe asumir que todos los casos de pantalla negra se solucionan únicamente por “activar MPEG-TS”.

También pueden causar este síntoma una pista de video ausente o incorrecta, un PID de video no reconocido, parámetros de codificación no compatibles, señal dañada, segmentos HLS defectuosos, problemas de decodificación por hardware o una incompatibilidad específica del dispositivo.

### 20.2 Objetivo

El reproductor de LUKKA Televisión deberá identificar y manejar correctamente los formatos de transporte y códecs que estén presentes en las listas y streams autorizados, tanto en Android TV como en Smartphone.

La meta es maximizar la compatibilidad y evitar que un canal quede en negro cuando exista una alternativa técnica compatible. No se debe prometer reproducción universal: ningún reproductor puede garantizar todos los formatos, perfiles, niveles, DRM y flujos defectuosos en todos los modelos Android. Los casos no compatibles deberán diagnosticarse y reportarse claramente.

### 20.3 Requisitos obligatorios del reproductor

1. Inspeccionar el formato real de los streams utilizados por los canales de prueba, sin deducir el códec únicamente por la extensión `.ts` o `.m3u8`.
2. Soportar correctamente listas M3U y reproducción HLS cuando el origen las entregue en esos formatos.
3. Soportar MPEG-TS como formato de transporte cuando el reproductor y el dispositivo puedan decodificar los códecs contenidos en él.
4. Usar una versión estable y compatible de AndroidX Media3 / ExoPlayer, o mantener el motor actual si la auditoría demuestra que es adecuado.
5. Verificar las capacidades de decodificación del dispositivo antes de seleccionar una ruta de reproducción, cuando la API lo permita.
6. Manejar correctamente selección de pistas, pistas de video y audio, cambios de resolución y cambios de variante HLS.
7. Implementar estados visibles de carga, buffering, error de manifiesto, error de red y error de decodificación. No dejar indefinidamente una pantalla negra sin diagnóstico.
8. Ante un fallo recuperable, reintentar la preparación del stream de forma limitada y controlada, evitando ciclos infinitos.
9. Permitir cambiar de canal y recuperar el reproductor sin reiniciar toda la aplicación.
10. Mantener la integración con el OSD y el zapping estilo decodificador.

### 20.4 Diagnóstico de pantalla negra con audio

Cuando haya audio y no haya imagen, el reproductor deberá registrar información técnica suficiente para identificar la causa:

- URL o identificador interno del canal, ocultando tokens y credenciales.
- Tipo de fuente: HLS, MPEG-TS directo u otro transporte.
- Contenedor detectado, si está disponible.
- Códec de video y audio detectados.
- Resolución, frecuencia de cuadros, perfil y nivel, cuando estén disponibles.
- Pistas disponibles y pista seleccionada.
- Decodificador utilizado: hardware o software, si la plataforma informa ese dato.
- Modelo del dispositivo y versión de Android.
- Error detallado del reproductor y etapa en que ocurrió.
- Si el audio continúa funcionando mientras falla el video.

Los logs no deben guardar contraseñas, tokens de acceso, URLs privadas completas ni códigos de activación. Para diagnóstico se usarán identificadores de canal y URLs redactadas.

### 20.5 Estrategia de compatibilidad y recuperación

El desarrollo deberá seguir esta secuencia:

1. Confirmar que el manifiesto o flujo contiene una pista de video válida.
2. Confirmar que el códec, perfil, nivel, resolución y formato de píxel son compatibles con el dispositivo.
3. Verificar que la pista de video correcta esté seleccionada y que no esté deshabilitada.
4. Revisar errores de decodificación por hardware y probar una ruta alternativa únicamente si el motor y el dispositivo la soportan.
5. Si se incorpora decodificación por software, hacerlo como capacidad opcional, evaluando rendimiento, consumo, temperatura, licencias y compatibilidad de arquitectura. No incluir bibliotecas FFmpeg ni extensiones de códecs sin revisar sus licencias y condiciones de distribución.
6. Si el origen entrega un formato que el dispositivo no puede decodificar, evaluar una conversión o transcodificación en servidor, únicamente para streams cuyo uso y transformación estén autorizados. La conversión debe producir un formato compatible con los dispositivos objetivo y dimensionarse para la capacidad real del servidor y la red.
7. Si no existe una ruta compatible, mostrar un mensaje breve y entendible, registrar el diagnóstico y permitir volver a intentar o cambiar de canal.

No se debe forzar indiscriminadamente el modo de decodificación por software: puede producir bajo rendimiento, calentamiento, consumo excesivo de batería o pérdida de fluidez, especialmente en TV Box de baja capacidad.

### 20.6 Requisitos para HLS y MPEG-TS

- Comprobar que los archivos M3U8 y sus segmentos sean accesibles y estén bien formados.
- Comprobar que las variantes de calidad declaradas en el manifiesto correspondan a streams reales.
- Verificar que los segmentos MPEG-TS contengan pistas de audio y video coherentes.
- Manejar correctamente discontinuidades, cambios de resolución y cambios de variante cuando estén presentes.
- No asumir que una lista M3U es un formato de video: es un índice de canales y direcciones de reproducción.
- No asumir que MPEG-TS determina por sí solo el códec.
- No modificar ni transcodificar listas o canales de terceros sin autorización.

### 20.7 Pruebas obligatorias de reproducción

Crear una matriz de pruebas con canales representativos de los formatos realmente autorizados y utilizados por LUKKA. Como mínimo, cuando estén presentes en las fuentes:

| Caso | Característica a probar | Resultado esperado |
|---|---|---|
| 1 | HLS con video H.264/AVC y audio compatible | Imagen y audio correctos |
| 2 | HLS con segmentos MPEG-TS y H.264/AVC | Imagen y audio correctos |
| 3 | MPEG-TS directo con códec compatible | Imagen y audio correctos |
| 4 | Video HEVC/H.265 en un dispositivo compatible | Reproducción correcta |
| 5 | Video HEVC/H.265 en un dispositivo no compatible | Error controlado; nunca bloqueo indefinido |
| 6 | Flujo con audio pero sin pista de video | Mensaje de fuente sin video |
| 7 | Pista de video presente pero no decodificable | Diagnóstico de códec/decodificador |
| 8 | Cambio entre variantes HLS | Reproducción estable |
| 9 | Segmento ausente o manifiesto defectuoso | Recuperación o error comprensible |
| 10 | Canal con resolución o perfil no soportado | Ruta alternativa compatible o aviso |
| 11 | Zapping repetido entre canales de formatos distintos | Sin bloqueo ni pantalla negra persistente |
| 12 | Reproducción en Android TV y Smartphone | Resultado documentado por dispositivo |

La matriz final debe reflejar los códecs que realmente aparezcan en los streams suministrados. No se deben declarar compatibles formatos que no hayan sido probados.

### 20.8 Criterios de aceptación adicionales

El módulo de reproducción se considerará aceptado cuando:

- Se haya identificado la causa de los canales reportados con audio y pantalla negra, o se haya documentado qué información falta para identificarla.
- Los streams de prueba autorizados se reproduzcan correctamente en los dispositivos objetivo compatibles.
- Los casos de códec no compatible presenten un error controlado y no una pantalla negra indefinida.
- Existan logs técnicos útiles y redactados para investigar fallos.
- El cambio de canal y la recuperación del reproductor funcionen después de un error.
- La corrección no rompa el arranque directo, el último canal visto, el OSD, el zapping ni el bloqueo por suscripción.

------------------------------------------------------------------------

## 21. Política confirmada/propuesta de corte y conectividad (versión 1.2)

### 21.1 Corte mensual

- Zona horaria oficial: `America/Bogota`.
- Hora propuesta de corte: **12:00:00 del día 5 de cada mes**.
- Período de gracia: **ninguno propuesto, pendiente de confirmación expresa**.
- La aplicación debe comparar fechas usando hora confiable del servidor y no depender exclusivamente del reloj del dispositivo.
- Si no existe renovación válida al llegar el corte, debe bloquear reproducción y navegación de canales y mostrar la pantalla de servicio suspendido.

### 21.2 Funcionamiento sin Internet

- Si no hay conectividad, la aplicación intentará reconectarse automáticamente cada **60 segundos**.
- Puede conservar y utilizar una licencia previamente validada y almacenada localmente de forma segura.
- La licencia almacenada debe ser verificable (firma digital o mecanismo equivalente) y contener usuario, plan, dispositivo autorizado, fecha de validación y expiración.
- El modo sin conexión **no puede extender la vigencia más allá de la expiración firmada ni superar el corte mensual**.
- Si la licencia vence durante una interrupción de Internet, la aplicación debe bloquearse al llegar la expiración conocida. No se debe conceder una gracia oculta.
- Al recuperar conectividad, validar inmediatamente con el servidor y actualizar la licencia local.
- Implementar protección contra retroceso del reloj del dispositivo; el reloj local por sí solo nunca autoriza una renovación.

### 21.3 Banco de usuarios y códigos

Se generó un inventario asociado al proyecto:

- 500 identificadores consecutivos desde `80105001` hasta `80105500`.
- 36 códigos mensuales por usuario.
- 18.000 códigos decimales únicos de 16 dígitos.
- Tres planes distribuidos equilibradamente: 50, 80 y 120 canales.
- Cada código está vinculado a un único usuario, un plan y un número de renovación (1 a 36).
- Los códigos se generaron con aleatoriedad criptográficamente segura y se comprobó la unicidad en el conjunto generado.

**Archivos asociados:**

- `LUKKA_Inventario_Usuarios_Codigos_Activacion.xlsx`: inventario administrativo legible.
- `LUKKA_Banco_Codigos_Activacion_TEMPORAL.json`: estructura JSON con usuarios, planes y códigos para importación temporal.

### 21.4 Estructura lógica del JSON

El JSON usa el esquema `lukka.subscription-code-bank`, versión `1.0.0`, con:

- `schema` y `schema_version` para identificar el formato.
- `generated_at_utc` para auditoría de generación.
- `environment` con valor `TEMPORARY_IMPORT_ONLY`.
- `timezone` y `subscription_policy` para el corte y la política offline.
- `plans` con identificador, nombre y cantidad de canales.
- `summary` con totales y rango de usuarios.
- `users[]`, cada uno con `user_id`, `plan_id`, `plan_name` y `monthly_codes[]`.
- Cada código contiene `activation_code`, `renewal_number`, `type` y `status`.
- `security_notice` recuerda que el archivo contiene secretos de activación.

### 21.5 Seguridad obligatoria del banco JSON

El JSON generado contiene los códigos reales en texto legible. Por tanto:

1. Es un artefacto temporal de migración/importación, no el mecanismo final de licenciamiento.
2. No debe incorporarse al APK, al ZIP público del código fuente ni a un repositorio público de GitHub.
3. Debe importarse a una base de datos del servidor mediante un proceso administrativo restringido.
4. El backend debe consumir cada código de forma atómica y de un solo uso, verificando usuario, plan, estado, renovación, vigencia y dispositivo autorizado.
5. Debe registrar auditoría de quién activó, cuándo, desde qué dispositivo y qué código (guardar referencia/huella, no exponer el secreto en logs).
6. El archivo y sus copias deben protegerse con acceso mínimo, cifrado en reposo y eliminación segura después de confirmar la importación y respaldo protegido.
7. No usar el JSON del APK como fuente de verdad: un atacante podría extraerlo y activar códigos sin autorización.

### 21.6 Asignación de planes generada

| Plan | Canales | Usuarios | Códigos |
|---|---:|---:|---:|
| `PLAN-50` | 50 | 167 | 6.012 |
| `PLAN-80` | 80 | 167 | 6.012 |
| `PLAN-120` | 120 | 166 | 5.976 |
| **Total** | — | **500** | **18.000** |

La asignación equilibrada es una distribución inicial técnica. Si la empresa desea vender planes en proporciones diferentes, debe aprobarse una nueva distribución antes de activar usuarios reales.

### 21.7 Decisiones aún pendientes

- Confirmación definitiva de que no habrá período de gracia.
- Número telefónico/WhatsApp/canal de soporte para la pantalla de bloqueo; no se inventará ni se mostrará un dato hasta que GRM CONEXIÓN lo suministre.
- Política comercial de reasignación de plan y reposición de códigos.
- Endpoint y credenciales de servidor de licencias, que se definirán fuera del APK y nunca se incluirán en el repositorio público.
- Logotipo oficial y recursos gráficos originales que se incorporarán cuando el usuario entregue los archivos.
- Proyecto Android base ZIP para integrar y probar las pantallas y validación.

