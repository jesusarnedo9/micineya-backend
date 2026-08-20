package com.arnedo.micine.controller;

import com.arnedo.micine.dto.ResenaRequest;
import com.arnedo.micine.service.ResenaService;
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
    public ResponseEntity dejarResena(@RequestBody ResenaRequest request, Principal principal) {
        resenaService.crearResena(principal.getName(), request);
        return ResponseEntity.ok("¡Reseña guardada exitosamente!");
    }

    @GetMapping("/pelicula/{tmdbId}")
    public ResponseEntity verResenasDePelicula(@PathVariable Long tmdbId) {
        return ResponseEntity.ok(resenaService.obtenerResenasPorPelicula(tmdbId));
    }
}