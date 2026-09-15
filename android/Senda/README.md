# Senda para Android

Aplicación Android nativa para crear y seguir un plan anual de lectura bíblica. El plan
y su progreso funcionan localmente; la red solo se usa para comprobar actualizaciones.

## Versión 1.2.0

- Color de énfasis aplicado correctamente a la navegación y los controles de Ajustes.
- Respaldo comprimido `.senda` del plan, el progreso y la traducción preferida.
- Restauración desde el primer inicio, desde Avanzado o al abrir un archivo compatible.
- Guardado directo en Descargas y opción inmediata para compartir el respaldo.
- Comprobación diaria y ligera de la versión más reciente publicada en GitHub Releases.
- Descarga directa del APK actualizado mediante el gestor de descargas de Android.

## Versión 1.1.0

- Traducción predeterminada configurable: RVC, NTV, TLAI, PDT o NBV.
- Las lecturas deuterocanónicas se mantienen siempre en TLAI.
- Contraste adaptativo en la tarjeta «Días leídos» del modo oscuro.
- Paleta Fucsia en sustitución de Turquesa.
- Navegación con iconos vectoriales propios y optimizados.

## Compilar

Requisitos: JDK 17 y Android SDK 37.

```powershell
.\gradlew.bat testDebugUnitTest assembleRelease lintRelease
```

El APK optimizado se genera en `app/build/outputs/apk/release/app-release.apk`.

En equipos con memoria limitada, conviene ejecutar las tres tareas por separado con
`--no-daemon --max-workers=1`.

## Decisión matemática documentada

Los cuatro Evangelios contienen 89 capítulos. Dos recorridos exactos suman 178, pero un año tiene 365 o 366 días. Para respetar la petición prioritaria de incluir al menos un capítulo evangélico cada día, cada semestre usa su secuencia indicada (temática aleatoria en el primero y cronológica por etapas en el segundo) y vuelve al inicio cuando se agota. La interfaz explica este criterio.

Al incluir los deuterocanónicos, la combinación de lectura diaria del Evangelio y cobertura total exige usar cinco lecturas en buena parte de los días laborables y cuatro en algunos fines de semana. Nunca se superan los máximos solicitados.
