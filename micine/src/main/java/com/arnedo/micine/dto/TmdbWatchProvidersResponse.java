package com.arnedo.micine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbWatchProvidersResponse(Map<String, Region> results) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Region(List<Provider> flatrate) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Provider(@JsonProperty("provider_id") Integer id,
                           @JsonProperty("provider_name") String nombre,
                           @JsonProperty("display_priority") Integer prioridad) {}
}
