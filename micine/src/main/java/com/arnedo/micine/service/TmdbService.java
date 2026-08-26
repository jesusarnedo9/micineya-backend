package com.arnedo.micine.service;

import com.arnedo.micine.dto.PeliculaDto;
import com.arnedo.micine.dto.TmdbResponse;
import com.arnedo.micine.dto.TmdbVideoResponse;
import com.arnedo.micine.dto.TmdbVideoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Service
public class TmdbService {

    private final RestTemplate restTemplate;

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.url}")
    private String apiUrl;

    public TmdbService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TmdbResponse obtenerPeliculasPopulares() {
        String url = apiUrl + "/movie/popular?language=es-ES&api_key=" + apiKey;
        TmdbResponse response = restTemplate.getForObject(url, TmdbResponse.class);

        if (response != null && response.getResults() != null) {
            asignarVideos(response.getResults());
        }
        return response;
    }

    public TmdbResponse getRecomendaciones(String generosTmdbIds) {
        String url = apiUrl + "/discover/movie?api_key=" + apiKey
                + "&language=es-ES&with_genres=" + generosTmdbIds;
        TmdbResponse response = restTemplate.getForObject(url, TmdbResponse.class);

        if (response != null && response.getResults() != null) {
            asignarVideos(response.getResults());
        }
        return response;
    }

    // --- MÉTODOS PRIVADOS PARA BUSCAR VIDEOS ---

    private void asignarVideos(List<PeliculaDto> peliculas) {
        for (PeliculaDto p : peliculas) {
            try {
                // 1. Buscamos trailer en español
                String videoUrlEs = apiUrl + "/movie/" + p.getId() + "/videos?api_key=" + apiKey + "&language=es-ES";
                TmdbVideoResponse videoRes = restTemplate.getForObject(videoUrlEs, TmdbVideoResponse.class);

                String key = extraerMejorVideo(videoRes);

                // 2. Fallback: Si no hay en español, buscamos en el idioma original (sin filtro de idioma)
                if (key == null) {
                    String videoUrlEn = apiUrl + "/movie/" + p.getId() + "/videos?api_key=" + apiKey;
                    TmdbVideoResponse videoResEn = restTemplate.getForObject(videoUrlEn, TmdbVideoResponse.class);
                    key = extraerMejorVideo(videoResEn);
                }

                p.setVideoKey(key);
            } catch (Exception e) {
                // Si falla un video, que no se caiga toda la lista. Pasa a la siguiente película.
                System.out.println("Error buscando video para la película: " + p.getId());
            }
        }
    }

    private String extraerMejorVideo(TmdbVideoResponse response) {
        if (response == null || response.getResults() == null) return null;

        for (TmdbVideoDto v : response.getResults()) {
            if ("YouTube".equalsIgnoreCase(v.getSite()) &&
                    ("Trailer".equalsIgnoreCase(v.getType()) || "Teaser".equalsIgnoreCase(v.getType()))) {
                return v.getKey();
            }
        }
        return null;
    }
}