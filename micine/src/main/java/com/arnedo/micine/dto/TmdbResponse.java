package com.arnedo.micine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbResponse {
    private List<PeliculaDto> results;

    public List<PeliculaDto> getResults() { return results; }
    public void setResults(List<PeliculaDto> results) { this.results = results; }
}