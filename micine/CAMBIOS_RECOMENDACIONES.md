# Recomendaciones de MiCineYa

## Comportamiento implementado

- La región inicial es Argentina (`AR`).
- `GET /api/peliculas/recomendadas` devuelve hasta 10 películas.
- Solo incluye disponibilidad por suscripción (`flatrate`) en las plataformas elegidas.
- Si el usuario todavía no completó onboarding, usa todas las plataformas soportadas.
- Una reseña/puntuación marca la película como vista y la excluye de esta sección.
- Las películas guardadas influyen priorizando candidatos relacionados de TMDB.
- Los géneros y las plataformas se combinan con lógica OR.
- `GET /api/peliculas/populares?page=1` admite paginación y sigue siendo el feed general sin filtros de usuario.
- Para video se priorizan TV spots, teasers, trailers y clips oficiales, en ese orden.

## Plataformas iniciales

| Plataforma | ID de proveedor TMDB |
| --- | ---: |
| Netflix | 8 |
| Prime Video | 119 |
| Disney+ | 337 |
| Max | 1899 |

Hibernate agrega la columna `tmdb_provider_id` mediante el modo `update`. El seeder actualiza las filas existentes por nombre, por lo que conserva sus IDs locales y las relaciones de onboarding ya guardadas.

## Verificación

- Compilación de Java 21 completada.
- Paquete ejecutable de Spring Boot generado.
- Contexto de Spring validado con H2 en memoria para no depender de PostgreSQL local.

Al mostrar disponibilidad de plataformas en el frontend debe agregarse la atribución requerida a JustWatch por TMDB.
