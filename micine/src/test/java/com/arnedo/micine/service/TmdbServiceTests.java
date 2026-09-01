package com.arnedo.micine.service;

import com.arnedo.micine.dto.PeliculaDto;
import com.arnedo.micine.dto.PerfilRecomendacion;
import com.arnedo.micine.dto.TmdbResponse;
import com.arnedo.micine.dto.TmdbVideoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TmdbServiceTests {

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
}
