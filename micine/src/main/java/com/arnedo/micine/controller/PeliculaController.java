package com.arnedo.micine.controller;

import com.arnedo.micine.dto.TmdbResponse;
import com.arnedo.micine.service.TmdbService;
import com.arnedo.micine.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

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
    public TmdbResponse getPopulares() {
        return tmdbService.obtenerPeliculasPopulares();
    }
    @GetMapping("/recomendadas")
    public ResponseEntity getRecomendadas(Principal principal) {
        // 1. Averiguamos qué géneros le gustan al usuario logueado
        String generosIds = usuarioService.getGenerosTmdbIds(principal.getName());

        // 2. Le pedimos a TMDB las películas que coincidan con esos gustos
        TmdbResponse recomendaciones = tmdbService.getRecomendaciones(generosIds);

        return ResponseEntity.ok(recomendaciones);
    }
}