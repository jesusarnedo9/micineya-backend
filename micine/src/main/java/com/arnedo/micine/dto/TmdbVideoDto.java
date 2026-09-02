package com.arnedo.micine.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true) // ¡Acá también!
public class TmdbVideoDto {
    private String key;
    private String site;
    private String type;
    private String name;
    private Boolean official;

    @JsonProperty("iso_639_1")
    private String languageCode;

    @JsonProperty("iso_3166_1")
    private String countryCode;

    @JsonProperty("published_at")
    private String publishedAt;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getOfficial() { return official; }
    public void setOfficial(Boolean official) { this.official = official; }
    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
