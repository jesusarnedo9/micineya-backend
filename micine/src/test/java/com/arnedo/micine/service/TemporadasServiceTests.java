package com.arnedo.micine.service;

import com.arnedo.micine.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TemporadasServiceTests {
    private static final LocalDate HOY = LocalDate.of(2026, 9, 10);
    private final RestTemplate client = mock(RestTemplate.class);
    private final TemporadasService service = new TemporadasService(client,
            "https://api.themoviedb.org/3", "test-only-secret");

    @Test
    void listaOrdenadaSinEspecialesYConIdentidadDeSerie() {
        when(client.getForObject(anyString(), eq(TmdbSerieDetalle.class)))
                .thenReturn(serie(temporada(2, 2), temporada(0, 3), temporada(1, 2)));
        var result = service.listar(123L);
        assertThat(result.mediaType()).isEqualTo(TipoContenido.SERIE);
        assertThat(result.temporadas()).extracting(TemporadasSerieResponse.Temporada::numero)
                .containsExactly(1, 2);
        verify(client).getForObject(contains("/tv/123?"), eq(TmdbSerieDetalle.class));
    }

    @Test
    void todosLosEpisodiosPublicadosPermitenMarcarLaTemporada() {
        var result = evaluar(List.of(episodio(1, HOY.minusDays(7)), episodio(2, HOY)));
        assertThat(result.disponibleParaMarcar()).isTrue();
        assertThat(result.episodiosEstrenados()).isEqualTo(2);
        assertThat(result.motivo()).isNull();
    }

    @Test
    void unEpisodioFuturoNoPermiteDarPorTerminadaLaTemporada() {
        var result = evaluar(List.of(episodio(1, HOY), episodio(2, HOY.plusDays(1))));
        assertThat(result.disponibleParaMarcar()).isFalse();
        assertThat(result.episodiosEstrenados()).isEqualTo(1);
        assertThat(result.motivo()).contains("por estrenar");
    }

    @Test
    void fechasDesconocidasYCatalogosIncompletosNoDanFalsosCompletados() {
        assertThat(evaluar(List.of(episodio(1, HOY), episodio(2, null))).disponibleParaMarcar()).isFalse();
        assertThat(evaluar(List.of(episodio(1, HOY))).disponibleParaMarcar()).isFalse();
        assertThat(evaluar(List.of()).disponibleParaMarcar()).isFalse();
        assertThat(TemporadasService.evaluar(123L, temporada(1, null),
                new TmdbTemporadaDetalle(1, null), HOY).disponibleParaMarcar()).isFalse();
        assertThat(TemporadasService.evaluar(123L, temporada(1, 0),
                new TmdbTemporadaDetalle(1, List.of()), HOY).disponibleParaMarcar()).isFalse();
    }

    @Test
    void episodiosDuplicadosOFueraDeRangoNoSeCuentanDosVeces() {
        assertThat(evaluar(List.of(episodio(1, HOY), episodio(1, HOY))).disponibleParaMarcar()).isFalse();
        assertThat(evaluar(List.of(episodio(1, HOY), episodio(3, HOY))).disponibleParaMarcar()).isFalse();
        assertThat(evaluar(List.of(episodio(1, HOY),
                new TmdbTemporadaDetalle.Episodio(1L, 2, HOY))).disponibleParaMarcar()).isFalse();
    }

    @Test
    void rechazaIdsInvalidosYEspecialesAntesDeConsultarTmdb() {
        assertThatThrownBy(() -> service.listar(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listar(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.consultarDisponibilidad(123L, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.consultarDisponibilidad(123L, -1))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    @Test
    void temporadaAjenaALaSerieNoSeConsulta() {
        when(client.getForObject(anyString(), eq(TmdbSerieDetalle.class))).thenReturn(serie(temporada(1, 2)));
        assertThatThrownBy(() -> service.consultarDisponibilidad(123L, 2))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(client, never()).getForObject(anyString(), eq(TmdbTemporadaDetalle.class));
    }

    @Test
    void consultaLaTemporadaSolicitadaYRechazaRespuestasConOtraIdentidad() {
        when(client.getForObject(anyString(), eq(TmdbSerieDetalle.class))).thenReturn(serie(temporada(1, 2)));
        when(client.getForObject(anyString(), eq(TmdbTemporadaDetalle.class)))
                .thenReturn(new TmdbTemporadaDetalle(1, List.of(
                        episodio(1, LocalDate.of(2020, 1, 1)), episodio(2, LocalDate.of(2020, 1, 2)))));
        assertThat(service.consultarDisponibilidad(123L, 1).disponibleParaMarcar()).isTrue();
        verify(client).getForObject(contains("/tv/123/season/1?"), eq(TmdbTemporadaDetalle.class));
        when(client.getForObject(anyString(), eq(TmdbTemporadaDetalle.class)))
                .thenReturn(new TmdbTemporadaDetalle(2, List.of()));
        assertThatThrownBy(() -> service.consultarDisponibilidad(123L, 1))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void erroresDeTmdbSonRecuperablesYNoExponenLaClave() {
        when(client.getForObject(anyString(), eq(TmdbSerieDetalle.class)))
                .thenThrow(new ResourceAccessException("url?api_key=test-only-secret"));
        assertThatThrownBy(() -> service.listar(123L))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).doesNotContain("test-only-secret", "api_key");
                });
        when(client.getForObject(anyString(), eq(TmdbSerieDetalle.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.listar(123L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private DisponibilidadTemporadaResponse evaluar(List<TmdbTemporadaDetalle.Episodio> episodios) {
        return TemporadasService.evaluar(123L, temporada(1, 2), new TmdbTemporadaDetalle(1, episodios), HOY);
    }
    private static TmdbSerieDetalle serie(TmdbSerieDetalle.Temporada... temporadas) {
        return new TmdbSerieDetalle(123L, "Serie de prueba", "Returning Series", null, List.of(temporadas));
    }
    private static TmdbSerieDetalle.Temporada temporada(int numero, Integer episodios) {
        return new TmdbSerieDetalle.Temporada((long) numero, "Temporada " + numero, numero, episodios, HOY);
    }
    private static TmdbTemporadaDetalle.Episodio episodio(int numero, LocalDate estreno) {
        return new TmdbTemporadaDetalle.Episodio((long) numero, numero, estreno);
    }
}
