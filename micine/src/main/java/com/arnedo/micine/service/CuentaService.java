package com.arnedo.micine.service;

import com.arnedo.micine.entity.Usuario;
import com.arnedo.micine.repository.FotoPerfilRepository;
import com.arnedo.micine.repository.ResenaRepository;
import com.arnedo.micine.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class CuentaService {
    private final UsuarioRepository usuarios;
    private final ResenaRepository resenas;
    private final FotoPerfilRepository fotos;
    private final PasswordEncoder encoder;

    public CuentaService(UsuarioRepository usuarios, ResenaRepository resenas,
                         FotoPerfilRepository fotos, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.resenas = resenas;
        this.fotos = fotos;
        this.encoder = encoder;
    }

    @Transactional
    public void cambiarPassword(String email, String actual, String nueva) {
        Usuario usuario = verificarPassword(email, actual);
        if (nueva == null || nueva.length() < 8 || nueva.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres y no superar 72 bytes");
        }
        if (encoder.matches(nueva, usuario.getPassword())) {
            throw new IllegalArgumentException("Elegí una contraseña diferente de la actual");
        }
        usuario.setPassword(encoder.encode(nueva));
        usuario.incrementarTokenVersion();
    }

    @Transactional
    public void eliminar(String email, String password) {
        Usuario usuario = verificarPassword(email, password);
        fotos.deleteById(usuario.getId());
        resenas.deleteAll(resenas.findByUsuarioEmail(email));
        resenas.flush();
        // JPA elimina las relaciones y colecciones del usuario, conservando el catálogo compartido.
        usuarios.delete(usuario);
        usuarios.flush();
    }

    private Usuario verificarPassword(String email, String password) {
        Usuario usuario = usuarios.findByEmailForUpdate(email)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        if (password == null || !encoder.matches(password, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
        return usuario;
    }
}
