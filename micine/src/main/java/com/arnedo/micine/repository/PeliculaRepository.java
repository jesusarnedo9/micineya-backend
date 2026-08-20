package com.arnedo.micine.repository;

import com.arnedo.micine.entity.Pelicula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PeliculaRepository extends JpaRepository<Pelicula, Long> {

    // Spring Boot hace la magia de armar el SQL solo con leer este nombre
    Optional<Pelicula> findByTmdbId(Long tmdbId);

}