package com.arnedo.micine.service;

import com.arnedo.micine.config.ContenidoSchemaMigration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class ContenidoSchemaMigrationTests {
    @Test
    void cambiaSoloLaUnicidadDeTmdbConservandoFilasFkYOtrasRestricciones() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:migration_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE peliculas (id bigint primary key, tmdb_id bigint not null unique, titulo varchar(255) unique)");
        jdbc.execute("CREATE TABLE resenas (id bigint primary key, pelicula_id bigint references peliculas(id))");
        jdbc.update("INSERT INTO peliculas VALUES (1, 123, 'Película anterior')");
        jdbc.update("INSERT INTO resenas VALUES (10, 1)");
        // Mismo DDL aditivo utilizado por la nueva columna de la entidad Hibernate.
        jdbc.execute("ALTER TABLE peliculas ADD COLUMN media_type varchar(16) DEFAULT 'PELICULA' NOT NULL");
        var migration = new ContenidoSchemaMigration(jdbc);
        migration.run(); migration.run();
        assertThat(jdbc.queryForObject("SELECT media_type FROM peliculas WHERE id=1", String.class)).isEqualTo("PELICULA");
        assertThat(jdbc.queryForObject("SELECT pelicula_id FROM resenas WHERE id=10", Long.class)).isEqualTo(1L);
        jdbc.update("INSERT INTO peliculas VALUES (2, 123, 'Serie nueva', 'SERIE')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO peliculas VALUES (3, 123, 'Otra', 'SERIE')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO peliculas VALUES (4, 456, 'Serie nueva', 'SERIE')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM peliculas", Long.class)).isEqualTo(2);
        jdbc.execute("SHUTDOWN");
    }
}
