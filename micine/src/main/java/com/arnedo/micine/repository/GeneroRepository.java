package com.arnedo.micine.repository;

import com.arnedo.micine.entity.Genero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneroRepository extends JpaRepository<Genero, Long> {
    java.util.Optional<Genero> findByNombre(String nombre);
    java.util.Optional<Genero> findByTmdbId(Integer tmdbId);
}
