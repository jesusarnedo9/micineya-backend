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
        return calcular(id, resenas.contarPeliculasDistintas(id));
    }

    public static ProgresoResponse calcular(Long id, long vistas) {
        if (vistas < 0) throw new IllegalArgumentException("El progreso no puede ser negativo");
        return new ProgresoResponse(id, vistas, 10, vistas / 10, (int) (vistas % 10), vistas / 10 + 1);
    }
}
