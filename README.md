# Sleep Monitor

Aplicacion Android para monitorizar sesiones de sueno usando acelerometro y microfono del dispositivo, generar un reporte local y dejar preparada la integracion futura de un motor de IA on-device.

## Estructura del repositorio

```text
/root
├── app/                  # Proyecto Android Studio con Compose y Gradle
├── ml/                   # Espacio reservado para entrenamiento, modelos y experimentos ML
└── README.md             # Descripcion general del proyecto
```

## Arquitectura general

- `app/src/main/java/com/example/sleepmonitor/ui`
  Contiene navegacion, pantallas Compose y ViewModels.
- `app/src/main/java/com/example/sleepmonitor/data`
  Contiene Room, entidades, DAOs y repositorios.
- `app/src/main/java/com/example/sleepmonitor/service`
  Contiene el `Foreground Service` que captura sensores durante la noche y cierra la sesion.
- `app/src/main/java/com/example/sleepmonitor/domain/sleep`
  Contiene el motor local desacoplado que hoy genera reportes y recomendaciones basadas en reglas y manana puede sustituirse por TFLite.
- `ml/`
  Mantiene la estructura de datos, scripts y documentacion para la parte de Machine Learning.

## Modulos principales de la app

- Autenticacion local con registro, login, recuperacion de contrasena preparada para backend y eliminacion de cuenta.
- Persistencia local con Room para usuarios, sesiones, fases, muestras de sensores, resumenes y recomendaciones.
- Sesion nocturna con `Foreground Service`, captura de acelerometro y microfono, cierre manual o por ventana inteligente y recuperacion de estado.
- Reporte final con puntuacion, fases detectadas, resumen de ruido/movimiento, recomendaciones y feedback del usuario.
- Perfil con aviso de privacidad, precision limitada y datos utiles para la futura capa IA.

## Integracion futura de IA

La app no incluye IA real en esta entrega. En su lugar, el flujo queda preparado para integrar un modelo on-device:

- Los datos crudos y agregados ya se almacenan de forma estructurada.
- El motor actual vive en `domain/sleep/RuleBasedSleepInsightsEngine.kt`, pensado para poder ser reemplazado por un motor TFLite.
- El feedback del usuario ya queda persistido para futuras calibraciones.

## Requisitos funcionales cubiertos

- Inicio de sesion, registro, recuperacion de contrasena, cierre de sesion y eliminacion de cuenta.
- Inicio y parada de una sesion de sueno.
- Captura local de acelerometro y nivel de ruido.
- Despertar inteligente basado en ventana y fase ligera estimada.
- Reporte final con puntuacion, fases, hora de fin y recomendaciones.
- Feedback opcional del usuario y almacenamiento de discrepancia frente a la nota calculada.
- Historial de recomendaciones con fallback a consejos generales.

## Ejecucion en Android Studio

1. Abre la carpeta raiz del repositorio en Android Studio.
2. Espera a que Gradle sincronice el proyecto.
3. Selecciona un emulador o dispositivo Android 8.0+.
4. Ejecuta la configuracion `app`.
5. Concede permisos de microfono y notificaciones cuando la app los solicite.

## Build por linea de comandos

Desde la raiz del proyecto:

```powershell
gradlew.bat assembleDebug
```

## Notas importantes

- Todo el procesamiento de la sesion se hace localmente.
- La estimacion de fases usa movimiento y ruido, por lo que no tiene la precision de un wearable con pulsometro.
- La carpeta `ml/ML_EXPERIMENTS.md` se mantiene como punto de documentacion para los experimentos de Machine Learning cuando se desarrollen.
