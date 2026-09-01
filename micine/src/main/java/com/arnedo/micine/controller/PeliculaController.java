package com.arnedo.micine.controller;

import com.arnedo.micine.dto.TmdbResponse;
import com.arnedo.micine.service.TmdbService;
import com.arnedo.micine.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public TmdbResponse getPopulares(@RequestParam(defaultValue = "1") int page) {
        return tmdbService.obtenerPeliculasPopulares(page);
    }
    @GetMapping("/recomendadas")
    public ResponseEntity getRecomendadas(Principal principal) {
        var perfil = usuarioService.getPerfilRecomendacion(principal.getName());
        TmdbResponse recomendaciones = tmdbService.getRecomendaciones(perfil);

        return ResponseEntity.ok(recomendaciones);
    }
}
