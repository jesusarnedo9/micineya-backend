package com.arnedo.micine.repository;

import com.arnedo.micine.entity.ReporteComunidad;
import com.arnedo.micine.entity.ReporteComunidad.Estado;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

public interface ReporteComunidadRepository extends JpaRepository<ReporteComunidad, Long> {
    List<ReporteComunidad> findTop50ByEstadoOrderByCreadoAsc(Estado estado);
    long countByDenuncianteIdAndCreadoAfter(Long id, LocalDateTime desde);
    boolean existsByDenuncianteIdAndUsuarioIdAndResenaIdAndEstado(Long id, Long usuario, Long resena, Estado estado);
    void deleteByResenaIdIn(Collection<Long> ids);
    @Modifying @Query("delete from ReporteComunidad r where r.denuncianteId = :id or r.usuarioId = :id")
    void eliminarCuenta(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from ReporteComunidad r where r.id = :id")
    Optional<ReporteComunidad> bloquear(Long id);
}
