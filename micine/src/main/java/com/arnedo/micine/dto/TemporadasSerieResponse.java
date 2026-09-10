package com.arnedo.micine.dto;

import java.time.LocalDate;
import java.util.List;

public record TemporadasSerieResponse(Long tmdbId, TipoContenido mediaType, String titulo,
        String posterPath, String estado, List<Temporada> temporadas) {
    public record Temporada(int numero, String nombre, Integer cantidadEpisodios, LocalDate estreno) {}
}
