package com.arnedo.micine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbSerieDetalle(Long id, String name, String status,
        @JsonProperty("poster_path") String posterPath, List<Temporada> seasons) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Temporada(Long id, String name,
            @JsonProperty("season_number") Integer numero,
            @JsonProperty("episode_count") Integer cantidadEpisodios,
            @JsonProperty("air_date") LocalDate estreno) {}
}
