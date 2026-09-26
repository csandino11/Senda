# Senda 2.0.0 — comprobación de entrega

- Dos variantes release: YouVersion (paquete original) y Universal (paquete independiente).
- 17 pruebas automatizadas correctas por variante: 34 ejecuciones en total.
- Compilación release con R8 y reducción de recursos; lint sin errores.
- Ambos APK firmados con el certificado de publicación existente.
- Integridad Git y firmas verificadas después de la interrupción.
- No se dispuso de un dispositivo Android conectado: la interacción con lectores externos,
  launchers e instalación requiere además comprobación en los dispositivos de prueba.

## Límites que conviene conocer

El plan conserva exactamente los capítulos y repeticiones seleccionados. Cuando su duración
hace matemáticamente imposible leer los 89 capítulos de los Evangelios cada cinco días
(o los 178 con repetición), se utiliza un intervalo mayor y equilibrado, sin inventar repeticiones.
La afinidad diaria utiliza etiquetas temáticas y referencias cruzadas; no equivale a una
exégesis exhaustiva de todos los capítulos.

La edición universal reconoce diez lectores conocidos, sin afirmar un ranking global de
popularidad. Solo utiliza rutas de pasaje donde hay integración identificada; en otras apps
abre el lector y copia la cita. Las traducciones dependen de cada lector. Personalizado en
YouVersion utiliza un enlace sin identificador de traducción y depende del soporte de esa app.

## Fondos

Se utilizó la habilidad imagegen en modo integrado, no la API/CLI. Los catorce fondos nuevos
se guardan en `app/src/main/res/drawable-nodpi/theme_*_alt.webp` y `theme_*_alt2.webp`.
Son imágenes WebP de 720 × 1280, optimizadas para tamaño. Junto a las siete originales,
hay tres escenas por temática. Renovar alterna esas escenas sin conexión; no genera
imágenes ilimitadas mediante IA en el teléfono.

El conjunto de instrucciones visuales pidió fotografías naturales verticales 9:16,
composición serena con espacio para la interfaz y sin texto, logos ni personas. Escenas:

| Temática | Primera variante | Segunda variante |
|---|---|---|
| Fe | Sendero y amanecer de montaña | Faro y costa |
| Amor | Olivar acogedor | Huerto en flor |
| Esperanza | Amanecer entre montañas | Amanecer sobre el mar |
| Oración | Colina tranquila | Colina a la hora azul |
| Sabiduría | Sendero de bosque | Escalones de piedra |
| Justicia | Aldea y camino | Plaza acogedora |
| Perdón | Puente en la naturaleza | Pradera y arcoíris |

## Actualización de instalaciones anteriores

Además de los dos APK identificados por edición, la publicación incluye `Senda-2.0.0.apk`
y `Senda-2.0.0-Actualizacion.apk`, copias exactas de YouVersion. GitHub ordenó los archivos
por nombre, no por momento de carga: se verifica que Actualizacion sea el primer APK
devuelto para los clientes anteriores que escogen el primer archivo. El nombre sin sufijo
mantiene la URL alternativa que construyen cuando GitHub limita la consulta de su API.
