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
import jakarta.validation.Valid;
import com.arnedo.micine.dto.ConfirmarCuentaRequest;
import com.arnedo.micine.dto.CambiarPasswordRequest;
import com.arnedo.micine.dto.FotoPerfilRequest;
import com.arnedo.micine.dto.FotoPerfilResponse;
import com.arnedo.micine.service.CuentaService;
import com.arnedo.micine.service.FotoPerfilService;

@RestController
@RequestMapping("/api/users")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final CuentaService cuentaService;
    private final FotoPerfilService fotoService;
    private final com.arnedo.micine.service.ProgresoService progreso;

    public UsuarioController(UsuarioService usuarioService, CuentaService cuentaService, FotoPerfilService fotoService,
                             com.arnedo.micine.service.ProgresoService progreso) {
        this.usuarioService = usuarioService;
        this.cuentaService = cuentaService;
        this.fotoService = fotoService;
        this.progreso = progreso;
    }

    @GetMapping("/me/progreso")
    public com.arnedo.micine.dto.ProgresoResponse progreso(Principal principal) {
        return progreso.propio(principal.getName());
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> cambiarPassword(Principal principal, @Valid @RequestBody CambiarPasswordRequest request) {
        cuentaService.cambiarPassword(principal.getName(), request.passwordActual(), request.passwordNueva());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> eliminarCuenta(Principal principal, @Valid @RequestBody ConfirmarCuentaRequest request) {
        cuentaService.eliminar(principal.getName(), request.passwordActual());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/foto")
    public ResponseEntity<FotoPerfilResponse> obtenerFoto(Principal principal) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(fotoService.obtener(principal.getName()));
    }

    @PutMapping("/me/foto")
    public ResponseEntity<FotoPerfilResponse> guardarFoto(Principal principal, @Valid @RequestBody FotoPerfilRequest request) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(fotoService.guardar(principal.getName(), request.base64()));
    }

    @DeleteMapping("/me/foto")
    public ResponseEntity<Void> quitarFoto(Principal principal) {
        fotoService.quitar(principal.getName());
        return ResponseEntity.noContent().build();
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
