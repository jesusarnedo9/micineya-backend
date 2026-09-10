# Series — etapa 2: biblioteca y progreso del backend

Implementado en el backend y conectado con el frontend. **Pendiente prueba integrada en teléfono y despliegue**. No se generó APK ni se desplegó.
Continúa la [base de catálogo](series-etapa-1.md). La verificación de correo sigue fuera de este bloque.

## Identidad y compatibilidad

La identidad externa es `(mediaType, tmdbId)`: `movie:123` y `tv:123` son distintos.
Se conserva la tabla y entidad `Pelicula` para no cambiar claves primarias ni relaciones existentes.
Los registros anteriores reciben tipo `PELICULA`; los nuevos de series usan `SERIE` internamente.
La API serializa esos tipos como `movie` y `tv`.

- Las rutas anteriores `/api/resenas`, `/api/users/favoritas` y recomendaciones de películas mantienen semántica de películas.
- La nueva app usará `/api/biblioteca` para contenido mixto. No mandar una serie a una ruta antigua de películas.
- En comunidad, cada publicación incluye `mediaType`; siguen vigentes consentimiento, reportes, moderación y bloqueos.
- Las guardadas son privadas; nunca se añaden al perfil público.
- El APK anterior no conoce temporadas ni el nuevo total de pochoclos. Conserva operaciones de películas,
  pero no se debe usar para comprobar el progreso combinado; esa presentación corresponde al próximo frontend.

## Biblioteca: sesión requerida

| Método | Ruta | Operación |
| --- | --- | --- |
| GET | `/api/biblioteca/favoritas` | Guardadas privadas de ambos tipos |
| POST | `/api/biblioteca/favoritas` | Guardar un contenido |
| DELETE | `/api/biblioteca/favoritas/{tipo}/{id}` | Quitar una guardada; repetible |
| GET | `/api/biblioteca/resenas` | Vistas/reseñas propias, mixtas y ordenadas por fecha de vista |
| POST | `/api/biblioteca/resenas` | Crear o editar una reseña general del contenido |
| DELETE | `/api/biblioteca/resenas/{tipo}/{id}` | Deshacer la vista, reseña y temporadas asociadas; repetible |
| GET | `/api/biblioteca/progreso` | Progreso propio combinado |

`tipo` en la URL acepta solamente `movie` o `tv`. El usuario se obtiene siempre de la sesión, no de un ID enviado por el cliente.

Ejemplo de reseña de serie:

```json
{
  "mediaType": "tv",
  "tmdbId": 123,
  "titulo": "Nombre de la serie",
  "posterPath": "/poster.jpg",
  "calificacion": 4,
  "comentario": "Me gustó la segunda temporada",
  "spoiler": false,
  "temporadasVistas": [1, 2]
}
```

Para películas, `temporadasVistas` debe ser `[]`. Para series, debe contener al menos una temporada regular positiva.
El conjunto enviado **reemplaza** la selección anterior: `[2]` quita T1 y conserva T2. Para quitar todo, usar DELETE.
La selección admite hasta 100 temporadas; la puntuación es de 1 a 5 y la reseña de hasta 500 caracteres.
Guardar una favorita usa solo `mediaType`, `tmdbId`, `titulo` y `posterPath`.

Para una serie nueva, título y poster se obtienen de TMDB. Al agregar temporadas, se verifican las nuevas con TMDB.
No se hace una consulta HTTP mientras se mantiene el bloqueo de la cuenta. Editar solo texto/puntuación o quitar temporadas
no depende de la disponibilidad de TMDB. Los especiales y temporadas con episodios pendientes o datos incompletos se rechazan.
Las fechas son las que informa el catálogo; no prueban consumo real ni disponibilidad por plataforma de cada temporada individual.

Una tarjeta por contenido. `fechaVista` indica cuándo se marcó la película o se agregó una temporada por última vez;
editar el comentario no mueve la tarjeta. `fechaActualizacion` sigue disponible para actividad de reseñas.
Los registros antiguos sin `fechaVista` usan la fecha de actualización que ya tenían; no se puede reconstruir una fecha original perdida.

## Pochoclos

`totalPochoclos = películas distintas vistas + temporadas regulares distintas vistas`.
`peliculasVistas` conserva su significado; se agregan `seriesVistas`, `temporadasVistas` y `totalPochoclos`.
Cada diez pochoclos completan un balde. Repetir una petición o editar una reseña no genera crédito extra;
quitar una temporada o deshacer una vista ajusta el total. No hay un contador incremental que pueda acumular duplicados.
La eliminación de cuenta borra también temporadas y relaciones; el catálogo compartido permanece.

## Recomendaciones de series

| Método | Ruta | Operación |
| --- | --- | --- |
| GET | `/api/series/recomendadas` | Hasta diez series; registra el lote mostrado |
| POST | `/api/series/recomendadas/renovar` | Renueva excluyendo `actualesIds` (hasta diez) |
| PUT | `/api/series/descartadas/{tmdbId}` | No me interesa durante 30 días |
| DELETE | `/api/series/descartadas/{tmdbId}` | Deshacer descarte |

Excluye series con temporadas vistas y descartes vigentes; prioriza opciones fuera de las últimas 50 series recomendadas.
No utiliza vistas, favoritos, descartes ni historial de películas para filtrar IDs de series.
El endpoint de consulta `/api/series/catalogo` también respeta esas exclusiones, pero no registra un nuevo lote.
Todas las respuestas mantienen `generosSinEquivalencia` para informar gustos sin una categoría de TV equivalente.

## Migración y despliegue pendiente

Hibernate agrega `peliculas.media_type` con default `PELICULA`, `resenas.fecha_vista`, la colección de temporadas
y las colecciones de recomendaciones/descartes de series. El seeder de géneros sigue siendo idempotente.
`ContenidoSchemaMigration` se ejecuta antes del seeder y antes de que Spring complete el arranque:

1. En PostgreSQL toma un bloqueo de esquema corto sobre `peliculas` para serializar la migración.
2. Asegura la restricción única `(media_type, tmdb_id)`.
3. Retira únicamente las restricciones UNIQUE cuya única columna es `tmdb_id`, identificadas en el catálogo del motor.
4. No borra filas, claves primarias, reseñas, favoritos ni otras restricciones.

Probado con H2 en modo PostgreSQL, incluyendo filas antiguas y relaciones. **No se ejecutó contra PostgreSQL de producción.**
Antes de desplegar: respaldar la base y probar el arranque sobre una copia PostgreSQL. No hace falta ejecutar SQL manual ni nuevas variables.
El usuario hará el push/despliegue cuando esté listo el frontend. No volver a un backend anterior a esta migración una vez guardadas series:
ese código interpreta todos los IDs como películas. Conservar el backend compatible o preparar una reversión con respaldo.

## Integrado en frontend (pendiente prueba en teléfono)

- Películas/Series en Para vos y ruleta, con cachés y lotes separados.
- Claves tipadas también en guardadas, reseñas y almacenamiento local; migrar los registros locales antiguos a `movie`.
- Selección de temporadas en el modal actual, sin una reseña o tarjeta por temporada.
- Perfil mixto con etiqueta Peli/Serie; comunidad con etiqueta de tipo.
- Animación del balde basada en `totalPochoclos`, no solamente `peliculasVistas`.
- Explicación visible de géneros sin equivalente. Las categorías exclusivas de TV quedan para una ampliación posterior.

## Pruebas

Pruebas automatizadas con TMDB simulado y H2: identidad compartida entre tipos, crédito por temporada,
edición/deshacer, series futuras, permisos, bloqueo, privacidad, borrado de cuenta, orden de perfil,
migración de restricciones y regresión de las funciones anteriores.
