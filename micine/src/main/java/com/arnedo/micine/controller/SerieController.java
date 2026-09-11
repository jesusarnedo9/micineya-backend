package com.arnedo.micine.controller;

import com.arnedo.micine.dto.CatalogoSeriesResponse;
import com.arnedo.micine.dto.TemporadasSerieResponse;
import com.arnedo.micine.dto.DisponibilidadTemporadaResponse;
import com.arnedo.micine.service.CatalogoSeriesService;
import com.arnedo.micine.service.TemporadasService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

/** Catálogo e historial de series independientes de los de películas. */
@RestController
@RequestMapping("/api/series")
public class SerieController {
    private final CatalogoSeriesService catalogo;
    private final TemporadasService temporadas;

    public SerieController(CatalogoSeriesService catalogo, TemporadasService temporadas) {
        this.catalogo = catalogo;
        this.temporadas = temporadas;
    }

    @GetMapping("/catalogo")
    public CatalogoSeriesResponse catalogo(Principal principal) {
        return catalogo.consultar(principal.getName());
    }

    @GetMapping("/recomendadas")
    public CatalogoSeriesResponse recomendadas(Principal principal) {
        return catalogo.recomendar(principal.getName(), java.util.Set.of(), true);
    }

    @GetMapping("/buscar")
    public com.arnedo.micine.dto.TmdbResponse buscar(@RequestParam(defaultValue = "") String query) {
        return catalogo.buscar(query);
    }

    @PostMapping("/recomendadas/renovar")
    public CatalogoSeriesResponse renovar(Principal principal,
            @jakarta.validation.Valid @RequestBody com.arnedo.micine.dto.RenovarRecomendacionesRequest request) {
        return catalogo.recomendar(principal.getName(), request.actualesIds(), true);
    }

    @PutMapping("/descartadas/{tmdbId}")
    public org.springframework.http.ResponseEntity<Void> descartar(Principal principal, @PathVariable Long tmdbId) {
        catalogo.descartar(principal.getName(), tmdbId, true);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @DeleteMapping("/descartadas/{tmdbId}")
    public org.springframework.http.ResponseEntity<Void> deshacerDescarte(Principal principal, @PathVariable Long tmdbId) {
        catalogo.descartar(principal.getName(), tmdbId, false);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @GetMapping("/{tmdbId}/temporadas")
    public TemporadasSerieResponse temporadas(@PathVariable Long tmdbId) {
        return temporadas.listar(tmdbId);
    }

    @GetMapping("/{tmdbId}/temporadas/{numero}")
    public DisponibilidadTemporadaResponse disponibilidad(@PathVariable Long tmdbId,
                                                          @PathVariable int numero) {
        return temporadas.consultarDisponibilidad(tmdbId, numero);
    }
}
