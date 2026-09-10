package com.arnedo.micine.repository;

import com.arnedo.micine.entity.RelacionUsuario;
import com.arnedo.micine.entity.RelacionUsuario.Tipo;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface RelacionUsuarioRepository extends JpaRepository<RelacionUsuario, Long> {
    List<RelacionUsuario> findByOrigenIdAndTipo(Long origen, Tipo tipo);
    @Query("select r from RelacionUsuario r where r.tipo = :tipo and (r.origen.id = :id or r.destino.id = :id)")
    List<RelacionUsuario> relacionadas(Long id, Tipo tipo);
    boolean existsByOrigenIdAndDestinoIdAndTipo(Long origen, Long destino, Tipo tipo);
    void deleteByOrigenIdAndDestinoIdAndTipo(Long origen, Long destino, Tipo tipo);
    void deleteByOrigenIdOrDestinoId(Long origen, Long destino);
    default void eliminarCuenta(Long id) { deleteByOrigenIdOrDestinoId(id, id); }
}
