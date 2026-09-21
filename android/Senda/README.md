# Senda para Android

Aplicación Android nativa para crear y seguir un recorrido completo de lectura bíblica
desde el día de su creación. El plan y su progreso funcionan localmente; la red solo se
usa para comprobar actualizaciones.

## Versión 1.7.1

- Fondo temático extendido a toda la vista Día, incluida su cabecera.
- Gradación adaptada a los modos claro y oscuro, con tarjetas translúcidas legibles.
- Aviso de rendimiento aclarado en la opción Fondo Dinámico.

## Versión 1.7.0

- Opciones de personalización desplegables antes de crear el primer plan.
- Tamaño de letra Normal o Grande, persistente y compatible con la escala del sistema.
- Siete fondos dinámicos originales, uno por temática, optimizados en WebP y opcionales.
- Paleta Lumbre en la posición anterior de Añil; Añil ocupa el lugar de Violeta.
- Pantalla de descarga con progreso, detección del APK vigente ya descargado y
  restauración del proceso tras cerrar o reiniciar la app.
- Instalación guiada mediante el instalador seguro de Android e indicaciones para Play
  Protect, con recuperación clara si la instalación se cancela o falla.

## Versión 1.6.0

- Controles de tema y color trasladados a Avanzado; Ajustes se reserva para futuras
  funciones y deja de ocupar un destino de navegación.
- Paleta Añil en sustitución de Coral y Cielo actualizado a un celeste más vibrante.
- Estado Parcial amarillo y claramente diferenciado de los días fuera del plan.
- Resumen de planes largos con el año de finalización cuando supera el año siguiente.
- Descubrimiento de actualizaciones corregido: una conexión fallida ya no cuenta como
  comprobación, se reintenta al volver a la app y existe una ruta pública alternativa.
- Textos iniciales y límites semanales de los ritmos simplificados.

## Versión 1.5.0

- Ritmos Suave, Moderado e Intensivo con límites distintos entre semana y fin de semana.
- Planes de 300 a 558 días, iniciados en la fecha de creación y capaces de abarcar
  varios años calendario.
- Calendario limitado al intervalo real del plan, con días externos deshabilitados.
- Dos ciclos exactos de Evangelios y Salmos, cuatro de Proverbios y orden cronológico
  estricto para el segundo ciclo evangélico.
- Capítulos extensos priorizados para fines de semana; Salmo 119 recibe carga reducida.
- Marcado automático al pulsar «Leer ahora…» y confirmación antes de volver a habilitar
  una lectura completada.
- Pantalla Día reiniciada automáticamente a Hoy al entrar en una fecha nueva.
- Navegación clara y oscura teñida por la paleta activa, sin el violeta predeterminado
  de Material.
- Compatibilidad con planes y respaldos creados en versiones anteriores.

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

Después de aplicar las exclusiones y repeticiones, el recorrido contiene exactamente
1.407 capítulos sin deuterocanónicos y 1.497 al incluirlos. Los ritmos distribuyen esa
misma cobertura en 520/558, 380/408 o 300/322 días, respectivamente.

La estimación de capítulos extensos usa conteos de versículos de conjuntos KJV
[canónico](https://github.com/renniemaharaj/kjv-bible) y
[con deuterocanónicos](https://github.com/aruljohn/Bible-kjv-1611) auditables; no se
incorpora ni redistribuye texto bíblico en la aplicación.
