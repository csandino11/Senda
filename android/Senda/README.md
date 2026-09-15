# Senda para Android

Aplicación Android nativa y offline para crear y seguir un plan anual de lectura bíblica.

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
