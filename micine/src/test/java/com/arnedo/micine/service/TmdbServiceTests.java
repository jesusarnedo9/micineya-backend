package com.arnedo.micine.service;

import com.arnedo.micine.dto.PeliculaDto;
import com.arnedo.micine.dto.PerfilRecomendacion;
import com.arnedo.micine.dto.TmdbResponse;
import com.arnedo.micine.dto.TmdbVideoResponse;
import com.arnedo.micine.dto.TmdbVideoDto;
import com.arnedo.micine.dto.TmdbWatchProvidersResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TmdbServiceTests {

    @Test
    void renovarExcluyeElLoteActualVistasYDescartadasYBuscaMasPaginas() {
        RestTemplate client = mock(RestTemplate.class);
        TmdbService service = new TmdbService(client);
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.themoviedb.org/3");
        ReflectionTestUtils.setField(service, "apiKey", "test");
        when(client.getForObject(anyString(), eq(TmdbResponse.class))).thenAnswer(call -> {
            String url = call.getArgument(0);
            TmdbResponse response = url.contains("page=4")
                    ? respuestaConIds(4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L)
                    : respuestaConIds(1L, 2L, 3L, 90L);
            response.setTotalPages(5);
            return response;
        });
        when(client.getForObject(anyString(), eq(TmdbVideoResponse.class))).thenReturn(videos());
        PerfilRecomendacion perfil = new PerfilRecomendacion(
                Set.of(28), Set.of(8), Set.of(1L), Set.of(), Set.of(2L), Set.of(90L));

        TmdbResponse response = service.getRecomendaciones(perfil, Set.of(3L));

        assertThat(response.getResults()).hasSize(10);
        assertThat(response.getResults()).extracting(PeliculaDto::getId)
                .doesNotContain(1L, 2L, 3L, 90L);
    }

    @Test
    void catalogoAgotadoNoReintroduceVistasDescartadasNiLoteActual() {
        RestTemplate client = mock(RestTemplate.class);
        TmdbService service = new TmdbService(client);
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.themoviedb.org/3");
        ReflectionTestUtils.setField(service, "apiKey", "test");
        when(client.getForObject(anyString(), eq(TmdbResponse.class)))
                .thenReturn(respuestaConIds(1L, 2L, 3L, 4L));
        when(client.getForObject(anyString(), eq(TmdbVideoResponse.class))).thenReturn(videos());

        TmdbResponse response = service.getRecomendaciones(new PerfilRecomendacion(
                Set.of(), Set.of(8), Set.of(1L), Set.of(), Set.of(2L), Set.of(4L)), Set.of(3L));

        assertThat(response.getResults()).extracting(PeliculaDto::getId).containsExactly(4L);
    }

    @Test
    void recomendacionesFiltranVistasLimitanADiezYPriorizanAfinidad() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TmdbService service = new TmdbService(restTemplate);
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.themoviedb.org/3");
        ReflectionTestUtils.setField(service, "apiKey", "test");

        when(restTemplate.getForObject(anyString(), eq(TmdbResponse.class))).thenAnswer(invocacion -> {
            String url = invocacion.getArgument(0);
            if (url.contains("/movie/999/recommendations")) {
                return respuestaConIds(3L, 4L);
            }
            return respuestaConIds(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
        });

        TmdbVideoResponse sinVideos = new TmdbVideoResponse();
        sinVideos.setResults(List.of());
        when(restTemplate.getForObject(anyString(), eq(TmdbVideoResponse.class))).thenReturn(sinVideos);

        PerfilRecomendacion perfil = new PerfilRecomendacion(
                Set.of(28, 35), Set.of(8, 119), Set.of(1L), Set.of(999L));

        TmdbResponse response = service.getRecomendaciones(perfil);

        assertThat(response.getResults()).hasSize(10);
        assertThat(response.getResults()).extracting(PeliculaDto::getId).doesNotContain(1L);
        assertThat(response.getResults()).extracting(PeliculaDto::getId).startsWith(3L, 4L);
    }

    @Test
    void popularesPriorizanTrailerLatinoCompletoSobreUnSpot() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TmdbService service = new TmdbService(restTemplate);
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.themoviedb.org/3");
        ReflectionTestUtils.setField(service, "apiKey", "test");

        when(restTemplate.getForObject(anyString(), eq(TmdbResponse.class)))
                .thenReturn(respuestaConIds(550L));
        when(restTemplate.getForObject(anyString(), eq(TmdbVideoResponse.class)))
                .thenReturn(videos(
                        video("spot-key", "TV Spot latino", "Trailer", true, "es", "MX"),
                        video("trailer-key", "Trailer oficial latino", "Trailer", true, "es", "MX")
                ));

        TmdbResponse response = service.obtenerPeliculasPopulares(1);

        assertThat(response.getResults().getFirst().getVideoKey()).isEqualTo("trailer-key");
    }

    @Test
    void recomendacionesLimitanTitulosDeUnaMismaSaga() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TmdbService service = new TmdbService(restTemplate);
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.themoviedb.org/3");
        ReflectionTestUtils.setField(service, "apiKey", "test");

        when(restTemplate.getForObject(anyString(), eq(TmdbResponse.class)))
                .thenReturn(respuestaConTitulos(
                        "Alien", "Aliens", "Alien 3", "Alien: Resurrección", "Alien: Covenant",
                        "Matrix", "Gladiador", "Titanic", "Her", "Memento", "Avatar", "Parásitos",
                        "Whiplash"));
        TmdbVideoResponse sinVideos = new TmdbVideoResponse();
        sinVideos.setResults(List.of());
        when(restTemplate.getForObject(anyString(), eq(TmdbVideoResponse.class))).thenReturn(sinVideos);

        TmdbResponse response = service.getRecomendaciones(
                new PerfilRecomendacion(Set.of(), Set.of(8), Set.of(), Set.of()));

        long cantidadAlien = response.getResults().stream()
                .map(PeliculaDto::getTitle)
                .filter(titulo -> titulo.toLowerCase().startsWith("alien"))
                .count();
        assertThat(response.getResults()).hasSize(10);
        assertThat(cantidadAlien).isLessThanOrEqualTo(2);
    }

    @Test
    void recomendacionesIncluyenSoloPlataformasElegidasDisponiblesEnArgentina() {
        RestTemplate client = mock(RestTemplate.class);
        TmdbService service = new TmdbService(client);
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.themoviedb.org/3");
        ReflectionTestUtils.setField(service, "apiKey", "test");
        when(client.getForObject(anyString(), eq(TmdbResponse.class))).thenReturn(respuestaConIds(10L));
        when(client.getForObject(anyString(), eq(TmdbVideoResponse.class))).thenReturn(videos());
        when(client.getForObject(anyString(), eq(TmdbWatchProvidersResponse.class))).thenAnswer(call -> {
            assertThat((String) call.getArgument(0)).contains("/movie/10/watch/providers");
            return new TmdbWatchProvidersResponse(Map.of(
                    "AR", new TmdbWatchProvidersResponse.Region(List.of(
                    new TmdbWatchProvidersResponse.Provider(119, "Amazon Prime Video", 2),
                    new TmdbWatchProvidersResponse.Provider(8, "Netflix", 1),
                    new TmdbWatchProvidersResponse.Provider(337, "Disney Plus", 3)))));
        });

        var resultado = service.getRecomendaciones(new PerfilRecomendacion(
                Set.of(28), Set.of(8, 119), Set.of(), Set.of()));
        service.getRecomendaciones(new PerfilRecomendacion(
                Set.of(28), Set.of(8, 119), Set.of(), Set.of()));

        assertThat(resultado.getResults().getFirst().getPlataformas())
                .containsExactly("Netflix", "Amazon Prime Video");
        verify(client, times(1)).getForObject(
                contains("/movie/10/watch/providers"), eq(TmdbWatchProvidersResponse.class));
    }

    @Test
    void buscarPeliculasDevuelveCoincidenciasAntesDeConsultarPlataformas() {
        RestTemplate client = mock(RestTemplate.class);
        TmdbService service = new TmdbService(client);
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.themoviedb.org/3");
        ReflectionTestUtils.setField(service, "apiKey", "test");
        when(client.getForObject(contains("/search/movie"), eq(TmdbResponse.class)))
                .thenReturn(respuestaConIds(10L));
        when(client.getForObject(contains("/movie/10/watch/providers"), eq(TmdbWatchProvidersResponse.class)))
                .thenReturn(new TmdbWatchProvidersResponse(Map.of(
                        "AR", new TmdbWatchProvidersResponse.Region(List.of(
                        new TmdbWatchProvidersResponse.Provider(8, "Netflix", 1),
                        new TmdbWatchProvidersResponse.Provider(337, "Disney Plus", 2))))));

        TmdbResponse resultado = service.buscarPeliculas("Alien");

        assertThat(resultado.getResults()).hasSize(1);
        assertThat(resultado.getResults().getFirst().getMediaType())
                .isEqualTo(com.arnedo.micine.dto.TipoContenido.PELICULA);
        assertThat(resultado.getResults().getFirst().getPlataformas()).isEmpty();
        verify(client).getForObject(contains("query=Alien"), eq(TmdbResponse.class));
        verify(client, never()).getForObject(
                contains("/movie/10/watch/providers"), eq(TmdbWatchProvidersResponse.class));

        assertThat(service.obtenerPlataformas(10L, com.arnedo.micine.dto.TipoContenido.PELICULA))
                .containsExactly("Netflix", "Disney Plus");
    }

    @Test
    void buscarSeriesDevuelveComoMaximoCincoCoincidencias() {
        RestTemplate client = mock(RestTemplate.class);
        TmdbService service = new TmdbService(client);
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.themoviedb.org/3");
        ReflectionTestUtils.setField(service, "apiKey", "test");
        when(client.getForObject(contains("/search/tv"), eq(TmdbResponse.class)))
                .thenReturn(respuestaConIds(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
        TmdbResponse resultado = service.buscarSeries("Dark");

        assertThat(resultado.getResults()).hasSize(5);
        assertThat(resultado.getResults()).allMatch(
                contenido -> contenido.getMediaType() == com.arnedo.micine.dto.TipoContenido.SERIE);
        verify(client).getForObject(contains("query=Dark"), eq(TmdbResponse.class));
        verify(client, never()).getForObject(anyString(), eq(TmdbWatchProvidersResponse.class));
    }

    private static TmdbVideoResponse videos(TmdbVideoDto... videos) {
        TmdbVideoResponse response = new TmdbVideoResponse();
        response.setResults(List.of(videos));
        return response;
    }

    private static TmdbVideoDto video(
            String key,
            String name,
            String type,
            boolean official,
            String language,
            String country) {
        TmdbVideoDto video = new TmdbVideoDto();
        video.setKey(key);
        video.setName(name);
        video.setType(type);
        video.setSite("YouTube");
        video.setOfficial(official);
        video.setLanguageCode(language);
        video.setCountryCode(country);
        return video;
    }

    private static TmdbResponse respuestaConIds(Long... ids) {
        List<PeliculaDto> peliculas = new ArrayList<>();
        for (Long id : ids) {
            PeliculaDto pelicula = new PeliculaDto();
            pelicula.setId(id);
            pelicula.setTitle("Película " + id);
            peliculas.add(pelicula);
        }
        return new TmdbResponse(peliculas);
    }

    private static TmdbResponse respuestaConTitulos(String... titulos) {
        List<PeliculaDto> peliculas = new ArrayList<>();
        for (int indice = 0; indice < titulos.length; indice++) {
            PeliculaDto pelicula = new PeliculaDto();
            pelicula.setId((long) indice + 1);
            pelicula.setTitle(titulos[indice]);
            peliculas.add(pelicula);
        }
        return new TmdbResponse(peliculas);
    }
}
