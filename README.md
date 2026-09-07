# Senda

Aplicación de lectura bíblica anual en español. React + Vinext, Cloudflare Workers y D1.

- 12 enfoques editoriales. Índice de temas por libro y capítulos, y conexiones bíblicas seleccionadas.
- 3–5 lecturas diarias, calendario local y años bisiestos.
- Exclusiones del programa; ciclos semestrales de Salmos y Evangelios y trimestrales de Proverbios.
- Evangelios: segunda vuelta como propuesta de armonización por etapas; algunos capítulos se dividen en pasajes sin duplicar versículos.
- Persistencia D1 identificada por una cookie aleatoria HttpOnly; no hay sincronización entre dispositivos ni recuperación tras borrar la cookie.
- PDF trimestral actual: cuatro páginas, Helvetica 12 puntos/A4 o 18 puntos/A3, texto comprimido y seleccionable. No contiene el texto bíblico.
- Lectura en YouVersion: RVC 146, NTV 127, TLAI 178. Se usa la edición publicada por el proveedor; el catálogo no confirma revisiones RVC 2025/2018. Las adiciones griegas usan libros separados.

## Comprobaciones

node --experimental-strip-types test-plan.ts
node test-api.mjs (servidor local y migración aplicados)
node node_modules/typescript/bin/tsc --noEmit
pnpm build

El verificador comprueba cobertura, exclusiones, conteos, intervalos NT, calendarios, ventanas de los ciclos y cobertura de los versículos de la armonización. Los tests recorren los 12 enfoques, ambos cánones y años normal/bisiesto; verifican también los cuatro trimestres y ambos tamaños de PDF. La afinidad editorial no equivale a garantizar citas directas entre todas las lecturas.

## Desarrollo

pnpm install
pnpm dev

Los scripts nativos opcionales de instalación están desactivados; la compilación utiliza los binarios distribuidos en las dependencias. El esquema está en db/schema.ts y sus migraciones en drizzle. No crear tablas durante las peticiones. La publicación Sites aplica las migraciones.

## Catálogos de traducciones

https://www.bible.com/es/versions/146
https://www.bible.com/es/versions/127
https://www.bible.com/es/versions/178

