package com.arnedo.micine.service;

import com.arnedo.micine.dto.PeliculaDto;
import com.arnedo.micine.dto.PerfilRecomendacion;
import com.arnedo.micine.dto.TmdbResponse;
import com.arnedo.micine.dto.TmdbVideoDto;
import com.arnedo.micine.dto.TmdbVideoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TmdbService {

    private static final String REGION_ARGENTINA = "AR";
    private static final int CANTIDAD_RECOMENDACIONES = 10;
    private static final int PAGINAS_CANDIDATAS = 3;
    private static final int MAX_FAVORITAS_PARA_AFINIDAD = 5;
    private static final List<String> IDIOMAS_TRAILER_LATINO = List.of("es-MX", "es-AR");
    private static final Set<String> PAISES_LATINOAMERICANOS = Set.of(
            "AR", "BO", "BR", "CL", "CO", "CR", "CU", "DO", "EC", "GT",
            "HN", "MX", "NI", "PA", "PE", "PR", "PY", "SV", "UY", "VE");

    private final RestTemplate restTemplate;

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.url}")
    private String apiUrl;

    public TmdbService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TmdbResponse obtenerPeliculasPopulares(int page) {
        int paginaSegura = Math.max(page, 1);
        String url = nuevaUrl("/movie/popular")
                .queryParam("language", "es-ES")
                .queryParam("region", REGION_ARGENTINA)
                .queryParam("page", paginaSegura)
                .build().encode().toUriString();

        TmdbResponse response = restTemplate.getForObject(url, TmdbResponse.class);
        if (response != null && response.getResults() != null) {
            asignarVideos(response.getResults());
        }
        return response == null ? new TmdbResponse(List.of()) : response;
    }

    public TmdbResponse getRecomendaciones(PerfilRecomendacion perfil) {
        if (perfil.plataformaIds().isEmpty()) {
            return new TmdbResponse(List.of());
        }

        Map<Long, PeliculaDto> candidatas = new LinkedHashMap<>();
        for (int pagina = 1; pagina <= PAGINAS_CANDIDATAS; pagina++) {
            TmdbResponse response = buscarCandidatas(perfil, pagina);
            if (response == null || response.getResults() == null) {
                continue;
            }

            response.getResults().stream()
                    .filter(pelicula -> pelicula.getId() != null)
                    .filter(pelicula -> !perfil.peliculasVistasIds().contains(pelicula.getId()))
                    .forEach(pelicula -> candidatas.putIfAbsent(pelicula.getId(), pelicula));
        }

        Map<Long, Integer> afinidadPorFavoritas = obtenerAfinidadPorFavoritas(perfil.peliculasFavoritasIds());
        List<PeliculaDto> elegidas = candidatas.values().stream()
                .sorted(Comparator.comparingInt(
                        (PeliculaDto pelicula) -> afinidadPorFavoritas.getOrDefault(pelicula.getId(), 0)
                ).reversed())
                .limit(CANTIDAD_RECOMENDACIONES)
                .collect(Collectors.toCollection(ArrayList::new));

        asignarVideos(elegidas);
        return new TmdbResponse(elegidas);
    }

    private TmdbResponse buscarCandidatas(PerfilRecomendacion perfil, int pagina) {
        UriComponentsBuilder url = nuevaUrl("/discover/movie")
                .queryParam("language", "es-ES")
                .queryParam("region", REGION_ARGENTINA)
                .queryParam("watch_region", REGION_ARGENTINA)
                .queryParam("with_watch_monetization_types", "flatrate")
                .queryParam("with_watch_providers", unirIds(perfil.plataformaIds()))
                .queryParam("include_adult", false)
                .queryParam("sort_by", "popularity.desc")
                .queryParam("vote_count.gte", 30)
                .queryParam("page", pagina);

        if (!perfil.generoIds().isEmpty()) {
            // El separador | representa OR: alcanza con coincidir con uno de los gustos.
            url.queryParam("with_genres", unirIds(perfil.generoIds()));
        }

        return restTemplate.getForObject(url.build().encode().toUriString(), TmdbResponse.class);
    }

    private Map<Long, Integer> obtenerAfinidadPorFavoritas(Set<Long> favoritasIds) {
        Map<Long, Integer> afinidad = new LinkedHashMap<>();
        favoritasIds.stream().limit(MAX_FAVORITAS_PARA_AFINIDAD).forEach(tmdbId -> {
            try {
                String url = nuevaUrl("/movie/" + tmdbId + "/recommendations")
                        .queryParam("language", "es-ES")
                        .queryParam("page", 1)
                        .build().encode().toUriString();
                TmdbResponse response = restTemplate.getForObject(url, TmdbResponse.class);
                if (response != null && response.getResults() != null) {
                    response.getResults().stream()
                            .map(PeliculaDto::getId)
                            .filter(java.util.Objects::nonNull)
                            .forEach(id -> afinidad.merge(id, 1, Integer::sum));
                }
            } catch (Exception ignored) {
                // Una favorita sin recomendaciones no debe romper todo el feed.
            }
        });
        return afinidad;
    }

    private String unirIds(Set<Integer> ids) {
        return ids.stream().sorted().map(String::valueOf).collect(Collectors.joining("|"));
    }

    private UriComponentsBuilder nuevaUrl(String path) {
        return UriComponentsBuilder.fromUriString(apiUrl)
                .path(path)
                .queryParam("api_key", apiKey);
    }

    private void asignarVideos(List<PeliculaDto> peliculas) {
        for (PeliculaDto pelicula : peliculas) {
            try {
                String key = null;
                for (String idioma : IDIOMAS_TRAILER_LATINO) {
                    key = extraerMejorVideo(buscarVideos(pelicula.getId(), idioma));
                    if (key != null) {
                        break;
                    }
                }

                // Si TMDB no tiene una versión latina, conservamos el trailer original
                // para que la película no quede sin contenido reproducible.
                if (key == null) {
                    key = extraerMejorVideo(buscarVideos(pelicula.getId(), null));
                }

                pelicula.setVideoKey(key);
            } catch (Exception ignored) {
                pelicula.setVideoKey(null);
            }
        }
    }

    private TmdbVideoResponse buscarVideos(Long peliculaId, String language) {
        UriComponentsBuilder url = nuevaUrl("/movie/" + peliculaId + "/videos");
        if (language != null) {
            url.queryParam("language", language);
        }
        return restTemplate.getForObject(url.build().encode().toUriString(), TmdbVideoResponse.class);
    }

    private String extraerMejorVideo(TmdbVideoResponse response) {
        if (response == null || response.getResults() == null) {
            return null;
        }

        return response.getResults().stream()
                .filter(video -> "YouTube".equalsIgnoreCase(video.getSite()))
                .filter(video -> puntuarVideo(video) > 0)
                .max(Comparator.comparingInt(this::puntuarVideo))
                .map(TmdbVideoDto::getKey)
                .orElse(null);
    }

    private int puntuarVideo(TmdbVideoDto video) {
        String nombre = video.getName() == null ? "" : video.getName().toLowerCase(Locale.ROOT);
        String tipo = video.getType() == null ? "" : video.getType().toLowerCase(Locale.ROOT);

        int puntaje;
        if (nombre.contains("tv spot") || nombre.contains("spot de tv")) {
            puntaje = 100;
        } else if ("trailer".equals(tipo)) {
            puntaje = 300;
        } else if ("teaser".equals(tipo)) {
            puntaje = 180;
        } else if ("clip".equals(tipo)) {
            puntaje = 60;
        } else {
            return 0;
        }

        if (Boolean.TRUE.equals(video.getOfficial())) {
            puntaje += 40;
        }
        if ("es".equalsIgnoreCase(video.getLanguageCode())) {
            puntaje += 40;
        }
        if (video.getCountryCode() != null
                && PAISES_LATINOAMERICANOS.contains(video.getCountryCode().toUpperCase(Locale.ROOT))) {
            puntaje += 60;
        }
        if (nombre.contains("latino")
                || nombre.contains("latinoamérica")
                || nombre.contains("latinoamerica")
                || nombre.contains("doblado")) {
            puntaje += 90;
        }

        return puntaje;
    }
}
