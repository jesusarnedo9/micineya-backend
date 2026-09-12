package com.arnedo.micine.service;

import com.arnedo.micine.dto.PeliculaDto;
import com.arnedo.micine.dto.PerfilRecomendacion;
import com.arnedo.micine.dto.TmdbResponse;
import com.arnedo.micine.dto.TmdbVideoDto;
import com.arnedo.micine.dto.TmdbVideoResponse;
import com.arnedo.micine.dto.TmdbWatchProvidersResponse;
import com.arnedo.micine.dto.TipoContenido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.text.Normalizer;
import java.util.stream.Collectors;

@Service
public class TmdbService {

    private static final String REGION_ARGENTINA = "AR";
    private static final int CANTIDAD_RECOMENDACIONES = 10;
    private static final int PAGINAS_CANDIDATAS = 3;
    private static final int MAX_PAGINAS_CANDIDATAS = 10;
    private static final int MAX_FAVORITAS_PARA_AFINIDAD = 5;
    private static final List<String> IDIOMAS_TRAILER_LATINO = List.of("es-MX", "es-AR");
    private static final Set<String> PAISES_LATINOAMERICANOS = Set.of(
            "AR", "BO", "BR", "CL", "CO", "CR", "CU", "DO", "EC", "GT",
            "HN", "MX", "NI", "PA", "PE", "PR", "PY", "SV", "UY", "VE");
    private static final Duration VIGENCIA_PLATAFORMAS = Duration.ofHours(12);

    private final RestTemplate restTemplate;
    private final Map<String, PlataformasCache> plataformasCache = new ConcurrentHashMap<>();

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
            asignarVideos(response.getResults(), TipoContenido.PELICULA);
        }
        return response == null ? new TmdbResponse(List.of()) : response;
    }

    public TmdbResponse buscarPeliculas(String consulta) {
        return buscarContenidos(consulta, TipoContenido.PELICULA);
    }

    public TmdbResponse buscarSeries(String consulta) {
        return buscarContenidos(consulta, TipoContenido.SERIE);
    }

    private TmdbResponse buscarContenidos(String consulta, TipoContenido tipo) {
        String termino = consulta == null ? "" : consulta.trim();
        if (termino.length() < 2) {
            return new TmdbResponse(List.of());
        }

        String url = nuevaUrl("/search/" + tipo.getCodigo())
                .queryParam("query", termino)
                .queryParam("language", "es-ES")
                .queryParam("region", REGION_ARGENTINA)
                .queryParam("include_adult", false)
                .queryParam("page", 1)
                .build().encode().toUriString();

        TmdbResponse response = restTemplate.getForObject(url, TmdbResponse.class);
        List<PeliculaDto> resultados = response == null || response.getResults() == null
                ? new ArrayList<>()
                : response.getResults().stream()
                        .filter(pelicula -> pelicula.getId() != null)
                        .limit(5)
                        .collect(Collectors.toCollection(ArrayList::new));
        resultados.forEach(contenido -> contenido.setMediaType(tipo));
        return new TmdbResponse(resultados);
    }

    public List<String> obtenerPlataformas(Long contenidoId, TipoContenido tipo) {
        tipo.clave(contenidoId);
        return plataformasDe(contenidoId, tipo).stream()
                .sorted(Comparator.comparing(
                        TmdbWatchProvidersResponse.Provider::prioridad,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(TmdbWatchProvidersResponse.Provider::nombre)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .distinct()
                .toList();
    }

    public TmdbResponse getRecomendaciones(PerfilRecomendacion perfil) {
        return getRecomendaciones(perfil, Set.of());
    }

    public TmdbResponse getRecomendaciones(PerfilRecomendacion perfil, Set<Long> actualesIds) {
        return getRecomendaciones(perfil, actualesIds, TipoContenido.PELICULA);
    }

    /** Todos los IDs del perfil y del lote deben pertenecer al tipo solicitado. */
    public TmdbResponse getRecomendaciones(PerfilRecomendacion perfil, Set<Long> actualesIds,
                                           TipoContenido tipo) {
        if (perfil.plataformaIds().isEmpty()) {
            return new TmdbResponse(List.of());
        }

        Map<Long, PeliculaDto> candidatas = new LinkedHashMap<>();
        for (int pagina = 1; pagina <= MAX_PAGINAS_CANDIDATAS; pagina++) {
            TmdbResponse response = buscarCandidatas(perfil, pagina, tipo);
            if (response == null || response.getResults() == null) {
                continue;
            }

            response.getResults().stream()
                    .filter(pelicula -> pelicula.getId() != null)
                    .filter(pelicula -> !perfil.peliculasVistasIds().contains(pelicula.getId()))
                    .filter(pelicula -> !perfil.peliculasDescartadasIds().contains(pelicula.getId()))
                    .filter(pelicula -> !actualesIds.contains(pelicula.getId()))
                    .forEach(pelicula -> candidatas.putIfAbsent(pelicula.getId(), pelicula));

            long nuevas = candidatas.keySet().stream()
                    .filter(id -> !perfil.recomendacionesRecientesIds().contains(id)).count();
            if ((pagina >= PAGINAS_CANDIDATAS && nuevas >= CANTIDAD_RECOMENDACIONES)
                    || response.getResults().isEmpty()
                    || (response.getTotalPages() != null && pagina >= response.getTotalPages())) {
                break;
            }
        }

        Map<Long, Integer> afinidadPorFavoritas = obtenerAfinidadPorFavoritas(perfil.peliculasFavoritasIds(), tipo);
        List<PeliculaDto> ranking = candidatas.values().stream()
                .sorted(Comparator.comparing((PeliculaDto pelicula) ->
                        perfil.recomendacionesRecientesIds().contains(pelicula.getId()))
                        .thenComparing(Comparator.comparingInt(
                        (PeliculaDto pelicula) -> afinidadPorFavoritas.getOrDefault(pelicula.getId(), 0)
                ).reversed()))
                .collect(Collectors.toCollection(ArrayList::new));
        List<PeliculaDto> elegidas = diversificarSagas(ranking);

        asignarPlataformas(elegidas, tipo, perfil.plataformaIds());
        asignarVideos(elegidas, tipo);
        return new TmdbResponse(elegidas);
    }

    private List<PeliculaDto> diversificarSagas(List<PeliculaDto> ranking) {
        List<PeliculaDto> elegidas = new ArrayList<>();
        Map<String, Integer> cantidadPorFamilia = new LinkedHashMap<>();
        Set<Long> idsElegidos = new HashSet<>();

        for (PeliculaDto pelicula : ranking) {
            String familia = claveDeFamilia(pelicula);
            if (cantidadPorFamilia.getOrDefault(familia, 0) >= 2) {
                continue;
            }
            elegidas.add(pelicula);
            idsElegidos.add(pelicula.getId());
            cantidadPorFamilia.merge(familia, 1, Integer::sum);
            if (elegidas.size() == CANTIDAD_RECOMENDACIONES) {
                return elegidas;
            }
        }

        // Si el catálogo es pequeño, completamos las diez aunque haya que repetir familia.
        for (PeliculaDto pelicula : ranking) {
            if (idsElegidos.add(pelicula.getId())) {
                elegidas.add(pelicula);
                if (elegidas.size() == CANTIDAD_RECOMENDACIONES) {
                    break;
                }
            }
        }
        return elegidas;
    }

    private String claveDeFamilia(PeliculaDto pelicula) {
        String titulo = pelicula.getTitle() == null ? "" : pelicula.getTitle();
        String normalizado = Normalizer.normalize(titulo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .trim();
        List<String> articulos = List.of("a", "an", "the", "el", "la", "los", "las", "un", "una");
        String raiz = java.util.Arrays.stream(normalizado.split("\\s+"))
                .filter(token -> !token.isBlank() && !articulos.contains(token))
                .findFirst()
                .orElse(String.valueOf(pelicula.getId()));

        if (raiz.length() > 5 && raiz.endsWith("s")) {
            raiz = raiz.substring(0, raiz.length() - 1);
        }
        return raiz;
    }

    private TmdbResponse buscarCandidatas(PerfilRecomendacion perfil, int pagina, TipoContenido tipo) {
        UriComponentsBuilder url = nuevaUrl("/discover/" + tipo.getCodigo())
                .queryParam("language", "es-ES")
                .queryParam("watch_region", REGION_ARGENTINA)
                .queryParam("with_watch_monetization_types", "flatrate")
                .queryParam("with_watch_providers", unirIds(perfil.plataformaIds()))
                .queryParam("include_adult", false)
                .queryParam("sort_by", "popularity.desc")
                .queryParam("vote_count.gte", 30)
                .queryParam("page", pagina);

        if (tipo == TipoContenido.PELICULA) {
            url.queryParam("region", REGION_ARGENTINA);
        } else {
            url.queryParam("include_null_first_air_dates", false)
                    .queryParam("first_air_date.lte", java.time.LocalDate.now(java.time.ZoneOffset.UTC));
        }

        if (!perfil.generoIds().isEmpty()) {
            // El separador | representa OR: alcanza con coincidir con uno de los gustos.
            url.queryParam("with_genres", unirIds(perfil.generoIds()));
        }

        return restTemplate.getForObject(url.build().encode().toUriString(), TmdbResponse.class);
    }

    private Map<Long, Integer> obtenerAfinidadPorFavoritas(Set<Long> favoritasIds, TipoContenido tipo) {
        Map<Long, Integer> afinidad = new LinkedHashMap<>();
        favoritasIds.stream().limit(MAX_FAVORITAS_PARA_AFINIDAD).forEach(tmdbId -> {
            try {
                String url = nuevaUrl("/" + tipo.getCodigo() + "/" + tmdbId + "/recommendations")
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

    private void asignarVideos(List<PeliculaDto> peliculas, TipoContenido tipo) {
        for (PeliculaDto pelicula : peliculas) {
            pelicula.setMediaType(tipo);
            try {
                String key = null;
                for (String idioma : IDIOMAS_TRAILER_LATINO) {
                    key = extraerMejorVideo(buscarVideos(pelicula.getId(), idioma, tipo));
                    if (key != null) {
                        break;
                    }
                }

                // Si TMDB no tiene una versión latina, conservamos el trailer original
                // para que la película no quede sin contenido reproducible.
                if (key == null) {
                    key = extraerMejorVideo(buscarVideos(pelicula.getId(), null, tipo));
                }

                pelicula.setVideoKey(key);
            } catch (Exception ignored) {
                pelicula.setVideoKey(null);
            }
        }
    }

    private void asignarPlataformas(List<PeliculaDto> contenidos, TipoContenido tipo,
                                    Set<Integer> plataformasElegidas) {
        for (PeliculaDto contenido : contenidos) {
            contenido.setPlataformas(plataformasDe(contenido.getId(), tipo).stream()
                    .filter(proveedor -> plataformasElegidas.contains(proveedor.id()))
                    .sorted(Comparator.comparing(
                            TmdbWatchProvidersResponse.Provider::prioridad,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(TmdbWatchProvidersResponse.Provider::nombre)
                    .filter(nombre -> nombre != null && !nombre.isBlank())
                    .distinct()
                    .toList());
        }
    }

    private List<TmdbWatchProvidersResponse.Provider> plataformasDe(Long contenidoId, TipoContenido tipo) {
        if (contenidoId == null) return List.of();
        String clave = tipo.getCodigo() + ":" + contenidoId;
        Instant ahora = Instant.now();
        PlataformasCache guardada = plataformasCache.get(clave);
        if (guardada != null && guardada.hasta().isAfter(ahora)) return guardada.proveedores();

        try {
            String url = nuevaUrl("/" + tipo.getCodigo() + "/" + contenidoId + "/watch/providers")
                    .build().encode().toUriString();
            TmdbWatchProvidersResponse respuesta = restTemplate.getForObject(
                    url, TmdbWatchProvidersResponse.class);
            var region = respuesta == null || respuesta.results() == null
                    ? null : respuesta.results().get(REGION_ARGENTINA);
            List<TmdbWatchProvidersResponse.Provider> proveedores = region == null || region.flatrate() == null
                    ? List.of() : List.copyOf(region.flatrate());
            plataformasCache.put(clave, new PlataformasCache(proveedores, ahora.plus(VIGENCIA_PLATAFORMAS)));
            return proveedores;
        } catch (Exception ignored) {
            // La disponibilidad agrega contexto, pero nunca debe bloquear las recomendaciones.
            return List.of();
        }
    }

    private record PlataformasCache(List<TmdbWatchProvidersResponse.Provider> proveedores, Instant hasta) {}

    private TmdbVideoResponse buscarVideos(Long peliculaId, String language, TipoContenido tipo) {
        UriComponentsBuilder url = nuevaUrl("/" + tipo.getCodigo() + "/" + peliculaId + "/videos");
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
