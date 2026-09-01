package com.arnedo.micine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbResponse {
    private List<PeliculaDto> results;
    private Integer page;

    @JsonProperty("total_pages")
    private Integer totalPages;

    public TmdbResponse() {
    }

    public TmdbResponse(List<PeliculaDto> results) {
        this.results = results;
        this.page = 1;
        this.totalPages = 1;
    }

    public List<PeliculaDto> getResults() { return results; }
    public void setResults(List<PeliculaDto> results) { this.results = results; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
}
