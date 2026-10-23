# Libreta 📓

App de notas para Android, nativa (Kotlin + Jetpack Compose), con las funciones de Evernote y un diseño **neo-brutalista**: papel crema, tinta carbón, sombras duras desplazadas, tipografía monoespaciada y acentos "crayón".

## Descargar el APK

Cada push compila un APK firmado con **GitHub Actions** y lo publica en **[Releases](../../releases/latest)**. Desde el teléfono:

1. Abrí la última release y descargá `Libreta-v1.0.N.apk`.
2. Abrilo y permití "Instalar apps desconocidas" si Android lo pide.
3. Las siguientes versiones se instalan encima, sin perder datos (todas usan la misma firma).

Requiere Android 8.0 (API 26) o superior.

## Funciones

| Evernote | Libreta |
|---|---|
| Notas con texto enriquecido | Editor por bloques con **markdown en vivo**: negrita, cursiva, subrayado, tachado, resaltado, código, enlaces; los marcadores se ocultan fuera del bloque activo |
| Encabezados, listas, casillas | H1/H2/H3, viñetas, numeradas (con sangría), casillas, citas, código, **destacados** de color, separadores |
| Tablas | Tablas editables (agregar/quitar filas y columnas) |
| Tareas con fecha, bandera y recordatorio | Bloque **Tarea** dentro de cualquier nota + pantalla de Tareas con filtros (hoy, 7 días, vencidas, marcadas…), deslizar para completar/eliminar |
| Recordatorios | Recordatorios de nota y de tarea con notificación, "Hecho" y "Posponer 10 min"; sobreviven al reinicio |
| Libretas y pilas | Libretas con color, ícono y pila; libreta predeterminada |
| Etiquetas | Etiquetas tipo *sticker* con color, filtros y atajos |
| Atajos | Notas, libretas, etiquetas y búsquedas guardadas como atajo |
| Inicio personalizable | Widgets: borrador rápido, nota fijada, recientes, tareas, atajos, recordatorios, libretas, etiquetas (reordenables) |
| Búsqueda | Búsqueda sin acentos con sintaxis `#etiqueta`, `libreta:`, `tiene:imagen/audio/archivo/tarea/casilla/recordatorio`, `creada:hoy/semana/mes`; recientes y guardadas |
| Escáner de documentos | Escáner ML Kit con recorte automático (imágenes + PDF) |
| Texto en imágenes | OCR: extraer texto de cualquier imagen |
| Adjuntos | Imágenes (galería/cámara), PDFs con vista previa, cualquier archivo, notas de voz con forma de onda |
| Dibujo a mano | Bocetos con crayones, grosores, goma y deshacer |
| Web Clipper | "Compartir → Libreta" desde el navegador: guarda título, portada, resumen y el texto de la página |
| Plantillas | 9 plantillas incluidas + guardar cualquier nota como plantilla |
| Historial de notas | Versiones automáticas y restauración |
| Enlaces entre notas | Bloque "Enlace a nota" + "Enlazada desde" (backlinks) en Información |
| Buscar en la nota | Resaltado de coincidencias con contador |
| Calendario | Vista mensual de notas, recordatorios y tareas |
| Papelera | Restaurar, eliminar para siempre, vaciado automático |
| Seguridad | PIN + huella/rostro para la app y para notas individuales |
| Exportar / compartir | PDF/impresión, Markdown, compartir como texto |
| Sincronización / respaldo | Copia completa en `.zip` (notas, libretas, etiquetas, tareas y adjuntos) para exportar e importar |
| Widget y accesos | Widget de captura rápida y atajos del ícono (nota, tarea, audio, escanear) |
| — | Extras: deshacer/rehacer, comandos "/", atajos de markdown al escribir, modo oscuro "Pizarra", papel rayado/cuadriculado/punteado, tres tipografías de editor |

## Diseño

Basado en el sistema de diseño de referencia (MotherDuck: "terminal en crayón sobre papel crema") y llevado más lejos:

- **Sombras duras** `-x / +y` sin desenfoque; los botones y las tarjetas *se hunden* en su sombra al tocarlos (con resorte y vibración).
- **JetBrains Mono** para la interfaz e **IBM Plex Sans/Serif** para leer y escribir notas largas.
- **Crayones** (cielo, canario, naranja, coral, menta, lila, lima, pervinca) para libretas, etiquetas y bordes "lápices en un vaso".
- **Ilustraciones a mano** dibujadas en código, con relleno *mal registrado* estilo risografía y animación sutil.
- **Marquesina** amarilla, tarjetas tipo libro para libretas, etiquetas como stickers levemente rotados.
- Tema oscuro **Pizarra** con sombras celestes.

## Compilar localmente

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Se necesita JDK 17 y el Android SDK (API 35). La clave `app/libreta-release.jks` viene incluida para que las builds se puedan instalar como actualización; para publicar en Play Store reemplazala por una propia mediante las variables `LIBRETA_STORE_PASSWORD`, `LIBRETA_KEY_ALIAS` y `LIBRETA_KEY_PASSWORD`.

## Arquitectura

- `model/` — modelo de bloques (JSON) y parser de formato en línea.
- `data/` — Room (notas, libretas, etiquetas, tareas, versiones, plantillas, búsquedas), DataStore y copias de seguridad.
- `ui/editor/` — editor por bloques, transformación visual de markdown, medios (audio, boceto, PDF).
- `ui/screens/` — Inicio, Notas, Tareas, Libretas, Etiquetas, Búsqueda, Calendario, Plantillas, Papelera, Ajustes.
- `ui/components/` — sistema de diseño (superficies presionables, botones, stickers, hojas, diálogos, ilustraciones).
- `reminders/` — alarmas y notificaciones. `widget/` — widget de inicio.

Fuentes bajo SIL Open Font License 1.1 (ver `app/src/main/assets/licenses`).
