# Senda

Senda es una aplicación Android de lectura bíblica anual en español. Genera un plan
personal para recorrer la Biblia durante el año, conecta lecturas relacionadas y guarda
todo el progreso localmente en el dispositivo.

[Descargar la versión más reciente](https://github.com/csandino11/Senda/releases/latest)

## Características

- Plan completo del 1 de enero al 31 de diciembre, con soporte para años bisiestos.
- Distribución equilibrada de Antiguo Testamento, Nuevo Testamento y Evangelios.
- Un capítulo de los Evangelios todos los días.
- Dos ciclos de Salmos y cuatro ciclos de Proverbios con órdenes diferentes.
- Siete temáticas para dar énfasis y relacionar las lecturas diarias.
- Deuterocanónicos opcionales y exclusiones específicas del programa de lectura.
- Vistas de día, semana y calendario mensual.
- Registro por capítulo y estados diarios: completo, parcial o no leído.
- Estadísticas precisas del avance anual.
- Seis paletas de énfasis y modos claro, oscuro o automático.
- Traducción predeterminada configurable: RVC, NTV, TLAI, PDT o NBV.
- Apertura prioritaria en YouVersion y alternativa a otra aplicación compatible.
- TLAI obligatoria para las lecturas deuterocanónicas.
- Respaldo comprimido del plan y el progreso en archivos `.senda`, restaurable en
  otra instalación o dispositivo.
- Comprobación silenciosa de nuevas versiones publicadas en GitHub una vez al día.

## Instalación

1. Abre la sección [Releases](https://github.com/csandino11/Senda/releases).
2. Descarga el APK de la versión deseada.
3. En Android, autoriza la instalación desde esa fuente cuando el sistema lo solicite.
4. Abre Senda y crea tu plan personal.

Senda requiere Android 8.0 (API 26) o posterior.

## Traducciones de YouVersion

| Sigla | Traducción | Identificador |
|---|---|---:|
| RVC | Reina Valera Contemporánea | 146 |
| NTV | Nueva Traducción Viviente | 127 |
| TLAI | Traducción al Lenguaje Actual Interconfesional | 178 |
| PDT | Palabra de Dios para Todos | 197 |
| NBV | Nueva Biblia Viva | 753 |

La aplicación no incluye ni redistribuye el texto de estas traducciones. Solamente abre
el capítulo correspondiente mediante enlaces de YouVersion.

## Tecnología

- Kotlin y Java 17.
- Jetpack Compose con Material 3.
- Android Gradle Plugin 9.4.
- `compileSdk` y `targetSdk` 37.
- Persistencia local mediante `SharedPreferences`.
- Respaldos portátiles mediante JSON comprimido con GZIP y firma de formato propia.
- Consulta ligera de GitHub Releases mediante las APIs nativas de Android.
- R8 y reducción de recursos habilitados para producir un APK ligero.
- El permiso de Internet se utiliza únicamente para la comprobación de actualizaciones;
  el plan y el progreso siguen almacenándose localmente.

## Estructura del repositorio

```text
android/Senda/   Aplicación Android nativa y versión distribuible
app/             Prototipo web inicial y API del generador
components/      Componentes de la interfaz web
db/              Esquema y persistencia del prototipo web
lib/             Motor y utilidades del prototipo web
```

La aplicación mantenida para Android se encuentra en [`android/Senda`](android/Senda).
El prototipo web se conserva como parte del historial completo del desarrollo.

## Compilar Android

Requisitos:

- JDK 17.
- Android SDK 37.

En Windows:

```powershell
cd android/Senda
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleRelease
.\gradlew.bat lintRelease
```

Sin una configuración privada de firma, Gradle produce una versión release sin firmar.
Para firmar localmente, crea `android/Senda/keystore.properties` con esta estructura:

```properties
storeFile=signing/senda-release.jks
storePassword=CONTRASEÑA_PRIVADA
keyAlias=senda
keyPassword=CONTRASEÑA_PRIVADA
```

El archivo de propiedades y el almacén de claves están excluidos mediante `.gitignore`.
Nunca deben incorporarse al repositorio.

## Verificación

La suite automatizada valida años normales y bisiestos, las siete temáticas, ambos
cánones, límites de lecturas, exclusiones, ciclos, cobertura y enlaces de las cinco
traducciones. Antes de cada publicación también se ejecutan lint, R8 y verificación de
la firma del APK.

## Versiones

- **1.2.0:** color de énfasis corregido en toda la interfaz, respaldo/restauración
  portátil del plan y el progreso, y comprobación diaria opcional de actualizaciones.
- **1.1.0:** traducción predeterminada, PDT y NBV, contraste oscuro corregido, paleta
  Fucsia e iconos vectoriales nuevos.
- **1.0.0:** primera versión Android con generador anual, calendario, seguimiento,
  temáticas, deuterocanónicos y personalización Material 3.

Consulta el historial completo y los APK en
[GitHub Releases](https://github.com/csandino11/Senda/releases).

