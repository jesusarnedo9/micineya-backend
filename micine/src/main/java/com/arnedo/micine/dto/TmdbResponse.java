package com.arnedo.micine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbResponse {
    private List results;

    public List getResults() { return results; }
    public void setResults(List results) { this.results = results; }
}