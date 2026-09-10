package com.arnedo.micine.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Migración aditiva de identidad. Hibernate agrega media_type con default PELICULA. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContenidoSchemaMigration implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    public ContenidoSchemaMigration(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public void run(String... args) {
        String producto = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>)
                c -> c.getMetaData().getDatabaseProductName());
        if (!"PostgreSQL".equals(producto) && !"H2".equals(producto)) {
            throw new IllegalStateException("Migración de contenidos: base de datos no soportada");
        }
        // Serializa despliegues simultáneos. No elimina filas ni cambia claves primarias/FK.
        if ("PostgreSQL".equals(producto)) jdbc.execute("LOCK TABLE peliculas IN ACCESS EXCLUSIVE MODE");
        Map<String, Set<String>> restricciones = new LinkedHashMap<>();
        jdbc.query("""
            select tc.constraint_name, k.column_name
            from information_schema.table_constraints tc
            join information_schema.key_column_usage k
              on k.constraint_catalog = tc.constraint_catalog
             and k.constraint_schema = tc.constraint_schema and k.constraint_name = tc.constraint_name
             and k.table_name = tc.table_name
            where lower(tc.table_name) = 'peliculas' and tc.table_schema = current_schema
              and tc.constraint_type = 'UNIQUE'
            """, rs -> {
                restricciones.computeIfAbsent(rs.getString(1), key -> new HashSet<>())
                        .add(rs.getString(2).toLowerCase(Locale.ROOT));
            });
        // Crear la protección nueva ANTES de retirar la restricción antigua.
        if (!restricciones.containsValue(Set.of("media_type", "tmdb_id"))) {
            jdbc.execute("ALTER TABLE peliculas ADD CONSTRAINT uk_contenido_tipo_tmdb UNIQUE (media_type, tmdb_id)");
        }
        restricciones.forEach((nombre, columnas) -> {
            if (columnas.equals(Set.of("tmdb_id"))) {
                // Identificador obtenido del catálogo del motor, nunca de un request.
                jdbc.execute("ALTER TABLE peliculas DROP CONSTRAINT \"" + nombre.replace("\"", "\"\"") + "\"");
            }
        });
    }
}
