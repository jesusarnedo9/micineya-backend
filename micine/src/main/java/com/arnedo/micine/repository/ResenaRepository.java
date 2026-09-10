package com.arnedo.micine.repository;

import com.arnedo.micine.entity.Resena;
import com.arnedo.micine.dto.TipoContenido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {
    List<Resena> findByPeliculaMediaTypeAndPeliculaTmdbId(TipoContenido tipo, Long tmdbId);
    default List<Resena> findByPeliculaTmdbId(Long tmdbId) {
        return findByPeliculaMediaTypeAndPeliculaTmdbId(TipoContenido.PELICULA, tmdbId);
    }

    @Query("select count(distinct r.pelicula.tmdbId) from Resena r where r.usuario.id = :usuarioId and r.pelicula.mediaType = com.arnedo.micine.dto.TipoContenido.PELICULA")
    long contarPeliculasDistintas(Long usuarioId);

    @Query("select r from Resena r join fetch r.usuario u join fetch r.pelicula where u.id in :autores and u.normasComunidadVersion = :version and (u.comunidadSuspendida = false or u.comunidadSuspendida is null) and (r.ocultadaModeracion = false or r.ocultadaModeracion is null) order by r.fechaActualizacion desc, r.id desc")
    List<Resena> publicaciones(Set<Long> autores, String version, org.springframework.data.domain.Pageable page);
    List<Resena> findByUsuarioEmail(String email);
    List<Resena> findByUsuarioEmailAndPeliculaMediaTypeAndPeliculaTmdbIdOrderByIdDesc(String email, TipoContenido tipo, Long tmdbId);
    default List<Resena> findByUsuarioEmailAndPeliculaTmdbId(String email, Long tmdbId) {
        return findByUsuarioEmailAndPeliculaMediaTypeAndPeliculaTmdbIdOrderByIdDesc(email, TipoContenido.PELICULA, tmdbId);
    }
    default Optional<Resena> findFirstByUsuarioEmailAndPeliculaTmdbIdOrderByIdDesc(String email, Long tmdbId) {
        return findByUsuarioEmailAndPeliculaTmdbId(email, tmdbId).stream().findFirst();
    }
    List<Resena> findByUsuarioIdAndPeliculaMediaType(Long usuarioId, TipoContenido tipo);

    @Query("select distinct r.pelicula.tmdbId from Resena r where r.usuario.email = :email and r.pelicula.mediaType = com.arnedo.micine.dto.TipoContenido.PELICULA")
    Set<Long> findPeliculasVistasTmdbIdsByUsuarioEmail(@Param("email") String email);

}
