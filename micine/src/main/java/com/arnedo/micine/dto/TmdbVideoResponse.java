package com.arnedo.micine.dto;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // ¡Esta es la magia!
public class TmdbVideoResponse {
    private List<TmdbVideoDto> results;

    public List<TmdbVideoDto> getResults() { return results; }
    public void setResults(List<TmdbVideoDto> results) { this.results = results; }
}