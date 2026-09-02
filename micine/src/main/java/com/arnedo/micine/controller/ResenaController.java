package com.arnedo.micine.controller;

import com.arnedo.micine.dto.ResenaRequest;
import com.arnedo.micine.service.ResenaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.arnedo.micine.dto.ResenaResponse;
import java.util.List;

import java.security.Principal;

@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    private final ResenaService resenaService;

    public ResenaController(ResenaService resenaService) {
        this.resenaService = resenaService;
    }

    @PostMapping
    public ResponseEntity<ResenaResponse> dejarResena(
            @Valid @RequestBody ResenaRequest request,
            Principal principal) {
        return ResponseEntity.ok(resenaService.guardarResena(principal.getName(), request));
    }

    @GetMapping("/pelicula/{tmdbId}")
    public ResponseEntity<List<ResenaResponse>> verResenasDePelicula(@PathVariable Long tmdbId) {
        return ResponseEntity.ok(resenaService.obtenerResenasPorPelicula(tmdbId));
    }

    @GetMapping("/mias")
    public ResponseEntity<List<ResenaResponse>> verMisResenas(Principal principal) {
        return ResponseEntity.ok(resenaService.obtenerMisResenas(principal.getName()));
    }

    @DeleteMapping("/pelicula/{tmdbId}")
    public ResponseEntity<Void> marcarComoNoVista(
            @PathVariable Long tmdbId,
            Principal principal) {
        resenaService.marcarComoNoVista(principal.getName(), tmdbId);
        return ResponseEntity.noContent().build();
    }
}
