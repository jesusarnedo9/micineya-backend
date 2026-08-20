package com.arnedo.micine.service;

import com.arnedo.micine.dto.TmdbResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TmdbService {

    private final RestTemplate restTemplate;

    // Spring inyecta automáticamente los valores del application.properties
    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.url}")
    private String apiUrl;

    public TmdbService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TmdbResponse obtenerPeliculasPopulares() {
        // Armamos la URL con idioma español y tu API Key
        String url = apiUrl + "/movie/popular?language=es-ES&api_key=" + apiKey;

        // Hacemos la petición GET y Spring lo convierte a nuestros DTOs mágicamente
        return restTemplate.getForObject(url, TmdbResponse.class);
    }

    public TmdbResponse getRecomendaciones(String generosTmdbIds) {
        String url = apiUrl + "/discover/movie?api_key=" + apiKey
                + "&language=es-ES&with_genres=" + generosTmdbIds;

        return restTemplate.getForObject(url, TmdbResponse.class);
    }
}