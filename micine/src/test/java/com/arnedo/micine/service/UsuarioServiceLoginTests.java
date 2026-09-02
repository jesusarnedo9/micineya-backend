package com.arnedo.micine.service;

import com.arnedo.micine.dto.AuthResponse;
import com.arnedo.micine.dto.LoginRequest;
import com.arnedo.micine.dto.RegistroRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.arnedo.micine.entity.Usuario;
import com.arnedo.micine.repository.UsuarioRepository;
import com.arnedo.micine.security.JwtService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UsuarioServiceLoginTests {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    private String username;
    private String email;
    private final String password = "password-segura";

    @BeforeEach
    void crearUsuario() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        username = "cinefilo_" + suffix;
        email = "cinefilo_" + suffix + "@example.com";

        RegistroRequest registro = new RegistroRequest();
        registro.setUsername(username);
        registro.setEmail(email);
        registro.setPassword(password);
        usuarioService.registrar(registro);
    }

    @Test
    void permiteIngresarConEmailSinImportarMayusculas() {
        AuthResponse response = usuarioService.login(login(email.toUpperCase()));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo(username);
    }

    @Test
    void permiteIngresarConUsernameSinImportarMayusculas() {
        AuthResponse response = usuarioService.login(login(username.toUpperCase()));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo(username);
    }

    @Test
    void cerrarSesionInvalidaElTokenEmitido() {
        AuthResponse response = usuarioService.login(login(email));
        Usuario antesDeSalir = usuarioRepository.findByEmail(email).orElseThrow();

        assertThat(jwtService.esTokenValido(
                response.getToken(), email, antesDeSalir.getTokenVersion())).isTrue();

        usuarioService.cerrarSesion(email);
        Usuario despuesDeSalir = usuarioRepository.findByEmail(email).orElseThrow();

        assertThat(jwtService.esTokenValido(
                response.getToken(), email, despuesDeSalir.getTokenVersion())).isFalse();
    }

    private LoginRequest login(String identifier) {
        LoginRequest request = new LoginRequest();
        request.setIdentifier(identifier);
        request.setPassword(password);
        return request;
    }
}
