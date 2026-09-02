package com.arnedo.micine.repository;

import com.arnedo.micine.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {
    List<Resena> findByPeliculaTmdbId(Long tmdbId);
    List<Resena> findByUsuarioEmail(String email);
    Optional<Resena> findFirstByUsuarioEmailAndPeliculaTmdbIdOrderByIdDesc(String email, Long tmdbId);

    @Query("select distinct r.pelicula.tmdbId from Resena r where r.usuario.email = :email")
    Set<Long> findPeliculasVistasTmdbIdsByUsuarioEmail(@Param("email") String email);

}
