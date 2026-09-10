# MiCineYa Backend

API REST de **MiCineYa**, una aplicación móvil que ayuda a elegir qué película ver a partir de las plataformas y los géneros preferidos por cada usuario.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Estado](https://img.shields.io/badge/estado-MVP_funcional-c51b29)](https://github.com/jesusarnedo9/micineya-backend)

El frontend del proyecto está disponible en [micineya_frontend](https://github.com/jesusarnedo9/micineya_frontend).

## Objetivos cumplidos

- Registro de usuarios e inicio de sesión mediante username o correo.
- Contraseñas protegidas con BCrypt.
- Autenticación stateless con access tokens JWT y renovación de sesión mediante refresh tokens.
- Invalidación de la sesión al cerrar sesión.
- Preferencias personalizadas por plataformas de streaming y géneros.
- Generación de diez recomendaciones disponibles por suscripción en Argentina.
- Renovación manual del lote, priorizando opciones fuera de las últimas 50 recomendaciones de cada usuario.
- Descartes mediante «No me interesa» durante 30 días, con posibilidad de deshacer.
- Exclusión automática de películas ya vistas.
- Priorización por afinidad con películas guardadas y limitación de títulos pertenecientes a una misma saga.
- Selección de trailers de YouTube, priorizando versiones en español latino cuando TMDB las ofrece.
- Gestión de películas guardadas.
- Creación y edición de puntuaciones y reseñas.
- Posibilidad de deshacer una película marcada como vista.
- Perfil con estadísticas, historial y contenido guardado.
- Foto de perfil opcional, comprimida y validada (JPEG 256 × 256, hasta 64 KiB).
- Cambio de contraseña con invalidación de las sesiones abiertas.
- Eliminación de cuenta y datos asociados desde la app o una página del backend.

## Tecnologías

| Área | Tecnología |
| --- | --- |
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1 |
| API | Spring Web MVC |
| Seguridad | Spring Security, JWT y BCrypt |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | PostgreSQL |
| Integración externa | TMDB API |
| Validaciones | Jakarta Bean Validation |
| Pruebas | JUnit, Spring Boot Test y H2 |
| Empaquetado | Maven y Docker |

## Arquitectura

El proyecto utiliza una arquitectura por capas:

```text
controller  →  service  →  repository  →  PostgreSQL
                    ↓
                 TMDB API
```

- **Controllers:** exponen los endpoints HTTP.
- **Services:** contienen la lógica de autenticación, recomendaciones, usuarios y reseñas.
- **Repositories:** administran el acceso a los datos mediante JPA.
- **DTOs:** definen los contratos de entrada y salida de la API.
- **Security:** valida los JWT y protege los recursos privados.

## Endpoints principales

| Método | Endpoint | Función |
| --- | --- | --- |
| `POST` | `/api/auth/registro` | Crear una cuenta |
| `POST` | `/api/auth/login` | Ingresar con username o correo |
| `POST` | `/api/auth/refresh` | Renovar la sesión |
| `POST` | `/api/auth/logout` | Cerrar e invalidar la sesión |
| `GET` | `/api/catalogos/plataformas` | Consultar plataformas disponibles |
| `GET` | `/api/catalogos/generos` | Consultar géneros disponibles |
| `GET` | `/api/peliculas/recomendadas` | Obtener las diez recomendaciones |
| `POST` | `/api/peliculas/recomendadas/renovar` | Pedir otro lote excluyendo sus `actualesIds` (hasta 10) |
| `PUT` | `/api/peliculas/descartadas/{tmdbId}` | Excluir una película durante 30 días |
| `DELETE` | `/api/peliculas/descartadas/{tmdbId}` | Deshacer el descarte |
| `GET/POST` | `/api/users/onboarding` | Consultar o guardar preferencias |
| `GET/POST` | `/api/users/favoritas` | Consultar o guardar películas |
| `DELETE` | `/api/users/favoritas/{tmdbId}` | Quitar una película guardada |
| `GET` | `/api/users/me` | Consultar el perfil del usuario |
| `PUT` | `/api/users/me/password` | Cambiar contraseña confirmando la actual |
| `DELETE` | `/api/users/me` | Eliminar cuenta confirmando la contraseña actual |
| `GET/PUT/DELETE` | `/api/users/me/foto` | Consultar, actualizar o quitar la foto de perfil |
| `POST` | `/api/resenas` | Crear o actualizar una puntuación y reseña |
| `GET` | `/api/resenas/mias` | Consultar el historial propio |
| `DELETE` | `/api/resenas/pelicula/{tmdbId}` | Marcar una película como no vista |

Salvo registro, login y renovación, los endpoints requieren un access token en el encabezado `Authorization: Bearer <token>`.

La página pública `/eliminar-cuenta.html` permite eliminar la cuenta sin instalar la app. Solicita usuario o correo, contraseña actual y confirmación explícita; reutiliza los endpoints autenticados. Estará disponible cuando se despliegue esta versión del backend.

La verificación de correo y la recuperación de contraseña siguen pendientes. Los cambios de este bloque y las instrucciones de despliegue están en [la guía de cuentas y perfil](docs/releases/1.1-cuentas-perfil.md).

## Configuración local

### Requisitos

- Java 21.
- Maven 3.9 o superior.
- PostgreSQL.
- Una API key de TMDB.

### Variables de entorno

| Variable | Descripción |
| --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL |
| `DB_USER` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |
| `JWT_SECRET` | Clave aleatoria de al menos 32 bytes para firmar tokens |
| `TMDB_API_KEY` | API key de TMDB |

El archivo [`micine/.env.example`](./micine/.env.example) muestra los nombres esperados sin incluir credenciales reales.

### Ejecución

```bash
git clone https://github.com/jesusarnedo9/micineya-backend.git
cd micineya-backend/micine
mvn spring-boot:run
```

La aplicación utiliza el puerto `8080` por defecto.

## Pruebas

```bash
cd micine
mvn test
```

La suite actual cubre el contexto de Spring, autenticación con username o correo, renovación e invalidación de tokens, reseñas y lógica de recomendaciones.

## Despliegue

El repositorio incluye un `Dockerfile` multi-stage que compila la aplicación con Maven y genera una imagen basada en Java 21. El backend del MVP se encuentra desplegado en Render y utiliza PostgreSQL como base de datos.

## Autor

Proyecto personal desarrollado por [Jesús Arnedo](https://github.com/jesusarnedo9) como backend del MVP full stack de MiCineYa.
