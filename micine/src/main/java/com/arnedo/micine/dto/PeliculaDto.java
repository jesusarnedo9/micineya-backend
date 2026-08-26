package com.arnedo.micine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PeliculaDto {
    private Long id;
    private String title;
    private String overview;
    private String videoKey;

    // La API manda "poster_path", pero en Java usamos camelCase.
    // Esta anotación hace la traducción automática.
    @JsonProperty("poster_path")
    private String posterPath;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public String getVideoKey() { return videoKey; }
    public void setVideoKey(String videoKey) { this.videoKey = videoKey; }
}