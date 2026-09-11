package com.arnedo.micine.controller;

import com.arnedo.micine.dto.TmdbResponse;
import com.arnedo.micine.dto.PeliculaDto;
import com.arnedo.micine.dto.RenovarRecomendacionesRequest;
import jakarta.validation.Valid;
import com.arnedo.micine.service.TmdbService;
import com.arnedo.micine.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;
import java.util.Set;

@RestController
@RequestMapping("/api/peliculas")
public class PeliculaController {

    private final TmdbService tmdbService;
    private final UsuarioService usuarioService;

    public PeliculaController(TmdbService tmdbService, UsuarioService usuarioService) {
        this.tmdbService = tmdbService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/populares")
    public TmdbResponse getPopulares(@RequestParam(defaultValue = "1") int page) {
        return tmdbService.obtenerPeliculasPopulares(page);
    }

    @GetMapping("/buscar")
    public TmdbResponse buscar(@RequestParam(defaultValue = "") String query) {
        return tmdbService.buscarPeliculas(query);
    }

    @GetMapping("/recomendadas")
    public ResponseEntity<TmdbResponse> getRecomendadas(Principal principal) {
        return recomendar(principal, Set.of());
    }

    @PostMapping("/recomendadas/renovar")
    public ResponseEntity<TmdbResponse> renovar(Principal principal,
            @Valid @RequestBody RenovarRecomendacionesRequest request) {
        return recomendar(principal, request.actualesIds());
    }

    @PutMapping("/descartadas/{tmdbId}")
    public ResponseEntity<Void> descartar(Principal principal, @PathVariable Long tmdbId) {
        usuarioService.descartarPelicula(principal.getName(), tmdbId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/descartadas/{tmdbId}")
    public ResponseEntity<Void> deshacerDescarte(Principal principal, @PathVariable Long tmdbId) {
        usuarioService.deshacerDescarte(principal.getName(), tmdbId);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<TmdbResponse> recomendar(Principal principal, Set<Long> actualesIds) {
        var perfil = usuarioService.getPerfilRecomendacion(principal.getName());
        TmdbResponse recomendaciones = tmdbService.getRecomendaciones(perfil, actualesIds);
        usuarioService.registrarRecomendaciones(principal.getName(), recomendaciones.getResults().stream()
                .map(PeliculaDto::getId).toList());

        return ResponseEntity.ok(recomendaciones);
    }
}
