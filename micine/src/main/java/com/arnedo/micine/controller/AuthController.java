package com.arnedo.micine.controller;

import com.arnedo.micine.dto.AuthResponse;
import com.arnedo.micine.dto.LoginRequest;
import com.arnedo.micine.dto.RegistroRequest;
import com.arnedo.micine.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.ok(usuarioService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Principal principal) {
        usuarioService.cerrarSesion(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
