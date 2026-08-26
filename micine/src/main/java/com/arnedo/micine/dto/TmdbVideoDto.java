package com.arnedo.micine.dto;

public class TmdbVideoDto {
    private String key; // Acá viene el ID de YouTube (ej: "dQw4w9WgXcQ")
    private String site; // "YouTube", "Vimeo", etc.
    private String type; // "Trailer", "Teaser", "Clip"

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}