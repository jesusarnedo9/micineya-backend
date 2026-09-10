package com.arnedo.micine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbTemporadaDetalle(@JsonProperty("season_number") Integer numero,
                                   List<Episodio> episodes) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Episodio(Long id, @JsonProperty("episode_number") Integer numero,
                           @JsonProperty("air_date") LocalDate estreno) {}
}
