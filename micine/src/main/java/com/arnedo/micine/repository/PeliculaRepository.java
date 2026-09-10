package com.arnedo.micine.repository;

import com.arnedo.micine.entity.Pelicula;
import com.arnedo.micine.dto.TipoContenido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PeliculaRepository extends JpaRepository<Pelicula, Long> {

    // Spring Boot hace la magia de armar el SQL solo con leer este nombre
    Optional<Pelicula> findByMediaTypeAndTmdbId(TipoContenido mediaType, Long tmdbId);

    default Optional<Pelicula> findByTmdbId(Long tmdbId) {
        return findByMediaTypeAndTmdbId(TipoContenido.PELICULA, tmdbId);
    }

}
