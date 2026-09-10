package com.arnedo.micine.service;

import com.arnedo.micine.dto.ProgresoResponse;
import com.arnedo.micine.repository.ResenaRepository;
import com.arnedo.micine.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProgresoService {
    private final ResenaRepository resenas;
    private final UsuarioRepository usuarios;
    public ProgresoService(ResenaRepository resenas, UsuarioRepository usuarios) {
        this.resenas = resenas; this.usuarios = usuarios;
    }

    public ProgresoResponse propio(String email) {
        return paraUsuario(usuarios.findByEmail(email).orElseThrow().getId());
    }

    // Uso interno: el perfil público verifica participación/bloqueos antes de pedir este resumen.
    public ProgresoResponse paraUsuario(Long id) {
        var series = new java.util.HashMap<Long, java.util.Set<Integer>>();
        resenas.findByUsuarioIdAndPeliculaMediaType(id, com.arnedo.micine.dto.TipoContenido.SERIE)
                .forEach(r -> series.computeIfAbsent(r.getPelicula().getId(), key -> new java.util.HashSet<>())
                        .addAll(r.getTemporadasVistas().stream().filter(n -> n != null && n > 0).toList()));
        long temporadas = series.values().stream().mapToLong(java.util.Set::size).sum();
        long vistas = resenas.contarPeliculasDistintas(id);
        long total = vistas + temporadas;
        return new ProgresoResponse(id, vistas, 10, total / 10, (int) (total % 10), total / 10 + 1,
                series.values().stream().filter(s -> !s.isEmpty()).count(), temporadas, total);
    }

    public static ProgresoResponse calcular(Long id, long vistas) {
        if (vistas < 0) throw new IllegalArgumentException("El progreso no puede ser negativo");
        return new ProgresoResponse(id, vistas, 10, vistas / 10, (int) (vistas % 10), vistas / 10 + 1, 0, 0, vistas);
    }
}
