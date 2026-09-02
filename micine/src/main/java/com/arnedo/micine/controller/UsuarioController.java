package com.arnedo.micine.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.arnedo.micine.dto.OnboardingRequest;
import com.arnedo.micine.dto.OnboardingStatusResponse;
import com.arnedo.micine.service.UsuarioService;
import com.arnedo.micine.dto.PeliculaRequest;
import com.arnedo.micine.dto.PerfilUsuarioResponse;
import com.arnedo.micine.entity.Pelicula;
import java.util.Set;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/users")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<PerfilUsuarioResponse> miPerfil(Principal principal) {
        return ResponseEntity.ok(usuarioService.obtenerPerfil(principal.getName()));
    }

    @PostMapping("/onboarding")
    public ResponseEntity guardarPreferencias(@RequestBody OnboardingRequest request, Principal principal) {
        // principal.getName() nos da el email del usuario logueado gracias al Token
        usuarioService.guardarPreferencias(principal.getName(), request);
        return ResponseEntity.ok("Preferencias guardadas exitosamente");
    }

    @GetMapping("/onboarding")
    public ResponseEntity<OnboardingStatusResponse> obtenerEstadoOnboarding(Principal principal) {
        return ResponseEntity.ok(usuarioService.getOnboardingStatus(principal.getName()));
    }

    @PostMapping("/favoritas")
    public ResponseEntity agregarFavorita(@RequestBody PeliculaRequest request, Principal principal) {
        usuarioService.agregarPeliculaFavorita(principal.getName(), request);
        return ResponseEntity.ok("¡Película agregada a favoritos con éxito!");
    }

    @GetMapping("/favoritas")
    public ResponseEntity obtenerFavoritas(Principal principal) {
        return ResponseEntity.ok(usuarioService.getPeliculasFavoritas(principal.getName()));
    }

    @DeleteMapping("/favoritas/{tmdbId}")
    public ResponseEntity eliminarFavorita(@PathVariable Long tmdbId, Principal principal) {
        usuarioService.eliminarPeliculaFavorita(principal.getName(), tmdbId);
        return ResponseEntity.ok("¡Película eliminada de favoritos!");
    }
}
