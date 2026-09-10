# Series — etapa 1: catálogo y temporadas

Nota histórica: esta etapa fue ampliada por la [etapa 2](series-etapa-2.md), que documenta el estado actual de biblioteca, progreso e historial.

Estado: base del backend. **Todavía no habilita series en la app ni registra temporadas vistas.**

## Implementado

- Catálogo de hasta diez series según las plataformas elegidas, suscripción en Argentina y géneros compatibles.
- Motor TMDB compartido con películas, pero rutas, trailers e identidad de contenido separados.
- Trailers: prioridad a español latino (`es-MX`, luego `es-AR`); versión original si no hay una alternativa latina. Puede no haber trailer.
- Géneros ampliados de 5 a 19 mediante actualización idempotente, conservando sus IDs y las preferencias existentes.
- Géneros con identificadores distintos para películas (`tmdbId`) y series (`tmdbTvId`).
- Lista de temporadas regulares, sin especiales (temporada 0).
- Consulta de disponibilidad para marcar una temporada: contrasta cantidad de episodios, identidad, números y fechas de estreno.
- Los endpoints nuevos requieren una sesión válida. Consultar el catálogo no cambia historial, guardadas ni pochoclos.

## Contrato de lectura

| Método | Ruta | Respuesta |
| --- | --- | --- |
| GET | `/api/series/catalogo` | `results` y `generosSinEquivalencia` |
| GET | `/api/series/{tmdbId}/temporadas` | Serie, estado y lista de temporadas regulares |
| GET | `/api/series/{tmdbId}/temporadas/{numero}` | Episodios catalogados/estrenados, `disponibleParaMarcar` y `motivo` |

Cada resultado del catálogo conserva `id`, `title`, `poster_path`, `overview`, `vote_average` y `videoKey`.
Agrega `mediaType: "tv"`. Los resultados de películas agregan `mediaType: "movie"` sin cambiar las rutas existentes.
TMDB entrega el nombre de una serie como `name`; el backend lo normaliza a `title`.

Identidad para la futura biblioteca: **tipo + ID**, por ejemplo `movie:123` y `tv:123` son contenidos distintos.
No enviar series a `/api/users/favoritas` ni `/api/resenas`: esas rutas siguen siendo exclusivamente de películas.
La separación del catálogo está implementada; la persistencia de biblioteca y reseñas de series queda para la siguiente etapa.

El catálogo de series es de solo lectura: no guarda un lote ni aplica historial de series vistas, descartadas o guardadas todavía.
No toma el historial de películas como si fuera de series. Si el catálogo tiene menos de diez coincidencias, devuelve menos.

### Géneros

Acción y Aventura corresponden a la categoría TV «Action & Adventure»; Ciencia Ficción y Fantasía a «Sci-Fi & Fantasy»;
Guerra a «War & Politics». Los géneros comunes conservan su equivalente directo.
Terror, Romance, Historia, Música, Suspenso y Película de TV no tienen una categoría directa de TV en esta tabla.
No se reemplazan por otra categoría arbitraria: se devuelven en `generosSinEquivalencia` para que la futura interfaz lo explique.
Si no hay ningún género compatible o ninguna plataforma seleccionada, la respuesta contiene una lista vacía sin consultar TMDB.
Las categorías exclusivas de TV (por ejemplo Reality y Kids) se incorporarán con la interfaz de preferencias por tipo;
no se ofrecen aún dentro de una app que solo muestra películas.

### Temporadas y límites

- Listar temporadas hace una consulta de detalle de serie, no una consulta por cada episodio o temporada.
- Consultar una temporada concreta hace una consulta de serie y otra de temporada.
- Una temporada vacía, con episodios duplicados, faltantes, sin fecha o con estrenos futuros no se habilita para marcar.
- `disponibleParaMarcar` describe los episodios **actualmente catalogados por TMDB**, usando fechas UTC, no horarios.
  No prueba que el usuario haya visto la temporada ni garantiza que TMDB no agregue más episodios después.
  Tampoco garantiza la disponibilidad de cada temporada individual dentro de una plataforma de Argentina.
- `estado` conserva el valor de TMDB. No se otorga una insignia de «serie terminada» a series en emisión.
- Los errores externos de temporada devuelven 404 o 503 con mensajes propios, sin exponer claves ni URLs internas.

## Compatibilidad y despliegue

No hay migraciones destructivas ni cambios en IDs de películas, reseñas o tablas de usuario.
Hibernate agrega una columna nullable `generos.tmdb_tv_id`; el seeder completa los equivalentes y agrega géneros faltantes.
No se necesitan nuevas claves, servicios de correo ni dependencias. No ejecutar SQL manual ni eliminar tablas.
La modificación de esquema se verifica aquí con H2; no se ejecutó un despliegue ni una migración contra PostgreSQL de producción.
El APK actual conserva el flujo de películas y podrá mostrar los géneros agregados cuando se despliegue el backend.
Este bloque no justifica generar un APK: falta integrar series de extremo a extremo.

## Siguiente etapa

1. Persistencia tipada de guardadas, vistas, descartes y reseñas; conservar los datos de películas y la compatibilidad del APK actual.
2. Una reseña general por serie y selección de temporadas completas dentro del modal existente; editar y deshacer sin duplicar crédito.
3. Pochoclos = películas distintas vistas + temporadas regulares distintas vistas; diez por balde.
4. Películas/Series en Para vos y en la ruleta; lotes e historiales independientes.
5. Perfil cronológico mixto con etiqueta Peli/Serie; comunidad con el mismo criterio y guardadas privadas.

## Verificación local

Pruebas enfocadas: `TmdbServiceTests`, `TemporadasServiceTests`, `CatalogoSeriesTests`, `ProgresoServiceTests` y `RecomendacionesUsuarioTests`.
Resultado del 10/09/2026: **27 pruebas aprobadas, sin fallos ni errores**.
Usan TMDB simulado y H2; no consumen la API real, no leen secretos de producción y no prueban todavía el teléfono.

Referencias: [catálogo TV](https://developer.themoviedb.org/reference/discover-tv),
[detalle de serie](https://developer.themoviedb.org/reference/tv-series-details),
[detalle de temporada](https://developer.themoviedb.org/reference/tv-season-details),
[géneros TV](https://developer.themoviedb.org/reference/genre-tv-list).
