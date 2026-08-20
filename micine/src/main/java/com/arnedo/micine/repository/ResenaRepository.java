package com.arnedo.micine.repository;

import com.arnedo.micine.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {
    List<Resena> findByPeliculaTmdbId(Long tmdbId);
    List<Resena> findByUsuarioEmail(String email);

}