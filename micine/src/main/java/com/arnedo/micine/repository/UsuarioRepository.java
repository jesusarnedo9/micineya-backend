package com.arnedo.micine.repository;

import com.arnedo.micine.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdForUpdate(Long id);

    @Query("select u from Usuario u where lower(u.username) like :patron escape '!' and u.normasComunidadVersion = :version and (u.comunidadSuspendida = false or u.comunidadSuspendida is null) and u.id not in :excluidos order by lower(u.username), u.id")
    java.util.List<Usuario> buscarPublicos(String patron, String version, java.util.Set<Long> excluidos, org.springframework.data.domain.Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.email = :email")
    Optional<Usuario> findByEmailForUpdate(@Param("email") String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    Optional<Usuario> findByUsernameIgnoreCase(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);

}
