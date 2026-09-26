# Senda web

Una sola aplicación web instalable para iPhone, iPad, Mac y navegadores de escritorio.
Es un proyecto independiente del prototipo web anterior. La fuente de las reglas del
plan es Senda Android 2.0.0, no `app/`, `lib/` ni la antigua API web del repositorio.

## Interfaz adaptable

La misma URL, datos y funciones se presentan según el sistema operativo: navegación
flotante y superficies de control inspiradas en Liquid Glass en iOS, iPadOS y macOS;
capas, barra de comandos y geometría inspiradas en Fluent para Windows. Se adapta
también al ancho de pantalla: barra inferior en iPhone, barra lateral en iPad/Mac/PC.
Son interpretaciones CSS, no los materiales nativos del sistema, que una web no puede
invocar directamente. Si no se detecta Apple o Windows se usa el estilo web neutro.
Para revisión local se puede agregar `?preview-platform=apple` o
`?preview-platform=windows`; este parámetro solo se atiende en localhost.

## Funciones

- Plan personal desde la fecha de creación: 1–4 capítulos entre semana y 1–3 en fines
  de semana, repeticiones configurables, temáticas, deuterocanónicos opcionales y
  exclusiones equivalentes a Android 2.0.0.
- Día, calendario multianual, seguimiento por capítulo, estadísticas, paletas, tamaño
  de letra, modo claro/oscuro y fondos temáticos renovables.
- Traducciones YouVersion RVC, NTV, TLAI, PDT, NBV y Personalizado. Las lecturas
  deuterocanónicas fuerzan TLAI.
- PDF local de los 45 días desde la descarga (o hasta el final del plan), en carta
  apaisada para letra Normal; legal apaisada, dos páginas, para Grande. Tres bloques
  FECHA / LIBRO / CAP. y encabezado en el color de énfasis. Tras prepararlo, la app
  ofrece descarga directa, vista previa y, si el navegador lo permite, compartirlo
  o guardarlo mediante el diálogo nativo. Algunos navegadores integrados bloquean
  las descargas; en ese caso se recomienda abrir la web en Safari o Chrome.
- Respaldo `.senda` en el formato 1 Android: bytes `SENDA\x01` + GZIP de JSON
  `{format,plan,completed,bibleVersion}`. Importa planes y progreso de Android.
- Sin registro: plan y progreso en el almacenamiento local del navegador. El respaldo
  es la forma de trasladarlos a otro dispositivo o protegerlos antes de borrar datos.
- Service worker con aplicación, canon y 21 fondos disponibles sin conexión. Los
  enlaces a YouVersion aún necesitan conexión, salvo contenido guardado allí.

## Enlaces de YouVersion en Apple

La app YouVersion existe para iPhone y iPad. La web intenta abrir el esquema
`youversion://bible?reference=LIBRO.CAPITULO.1&version=SIGLA` al tocar una lectura
en iOS/iPadOS, y después usa el enlace HTTPS de Bible.com si el navegador sigue
visible. El esquema no está documentado de forma pública y su parámetro de versión
necesita verificación en dispositivos físicos. El enlace HTTPS sí indica la versión
y el capítulo. En macOS y otros navegadores se usa el enlace HTTPS directamente.
Personalizado no fuerza una versión en el esquema nativo; en web la URL sin
identificador depende de cómo la procese Bible.com.

## Desarrollo

No hay dependencias de producción. Sirve `dist/` con un servidor HTTP local:

```powershell
python -m http.server 4173 --directory dist
```

Abre `http://localhost:4173`. `tools/check.mjs` verifica seis combinaciones
significativas del generador, cobertura, límites y la creación de PDF. El PDF de
ejemplo se guarda en `../output/pdf/` como artefacto de revisión.

## Mantenerla sincronizada con Android

1. Actualiza primero el motor y el canon de Android.
2. Lleva cambios equivalentes a `dist/catalog.js` y `dist/plan.js`.
3. Compara planes de ambas plataformas para las mismas combinaciones y semillas:
   capítulos cubiertos, exclusiones, repeticiones, límites diarios y fechas.
   El orden concreto puede variar por el generador aleatorio de cada plataforma.
4. Prueba la importación `.senda` en ambos sentidos antes de publicar.
5. Actualiza `dist/sw.js` si cambian archivos precargados y publica el sitio.
   El service worker usa la red para el código cuando hay conexión y caché sin red.

Para reducir trabajo en próximas versiones, la siguiente mejora de arquitectura es
extraer el canon y los casos de prueba a un paquete de datos compartido dentro del
repositorio y generar ambas representaciones desde ese origen.
