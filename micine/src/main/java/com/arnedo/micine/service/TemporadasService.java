package com.arnedo.micine.service;

import com.arnedo.micine.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
public class TemporadasService {
    private final RestTemplate client;
    private final String apiUrl;
    private final String apiKey;

    public TemporadasService(RestTemplate client, @Value("${tmdb.api.url}") String apiUrl,
                              @Value("${tmdb.api.key}") String apiKey) {
        this.client = client;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public TemporadasSerieResponse listar(Long serieId) {
        var serie = detalleSerie(serieId);
        return respuesta(serie);
    }

    public TemporadasSerieResponse validarParaRegistro(Long serieId, java.util.Set<Integer> numeros) {
        if (numeros == null || numeros.size() > 100 || numeros.stream().anyMatch(n -> n == null || n <= 0)) {
            throw new IllegalArgumentException("Las temporadas no son válidas");
        }
        var serie = detalleSerie(serieId);
        for (int numero : numeros) {
            var disponibilidad = consultarDisponibilidad(serie, numero);
            if (!disponibilidad.disponibleParaMarcar()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Temporada " + numero + ": " + disponibilidad.motivo());
            }
        }
        return respuesta(serie);
    }

    private TemporadasSerieResponse respuesta(TmdbSerieDetalle serie) {
        var temporadas = temporadasRegulares(serie).stream()
                .sorted(Comparator.comparing(TmdbSerieDetalle.Temporada::numero))
                .map(t -> new TemporadasSerieResponse.Temporada(t.numero(), t.name(),
                        t.cantidadEpisodios(), t.estreno())).toList();
        return new TemporadasSerieResponse(serie.id(), TipoContenido.SERIE, serie.name(),
                serie.posterPath(), serie.status(), temporadas);
    }

    /** Solo consulta metadatos. No registra que el usuario haya visto una temporada. */
    public DisponibilidadTemporadaResponse consultarDisponibilidad(Long serieId, int numero) {
        TipoContenido.SERIE.clave(serieId);
        if (numero <= 0) {
            throw new IllegalArgumentException("Elegí una temporada regular; los especiales no cuentan");
        }
        var serie = detalleSerie(serieId);
        return consultarDisponibilidad(serie, numero);
    }

    private DisponibilidadTemporadaResponse consultarDisponibilidad(TmdbSerieDetalle serie, int numero) {
        Long serieId = serie.id();
        var resumen = temporadasRegulares(serie).stream().filter(t -> t.numero() == numero)
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No encontramos esa temporada"));
        var detalle = consultar("/tv/" + serieId + "/season/" + numero, TmdbTemporadaDetalle.class);
        if (!Objects.equals(detalle.numero(), numero)) {
            throw catalogoNoDisponible();
        }
        return evaluar(serieId, resumen, detalle, LocalDate.now(ZoneOffset.UTC));
    }

    static DisponibilidadTemporadaResponse evaluar(Long serieId, TmdbSerieDetalle.Temporada resumen,
                                                    TmdbTemporadaDetalle detalle, LocalDate hoy) {
        List<TmdbTemporadaDetalle.Episodio> episodios = detalle.episodes() == null
                ? List.of() : detalle.episodes();
        int esperados = resumen.cantidadEpisodios() == null ? 0 : Math.max(0, resumen.cantidadEpisodios());
        var numeros = new HashSet<Integer>();
        var ids = new HashSet<Long>();
        int estrenados = 0;
        boolean datosCompletos = esperados > 0 && episodios.size() == esperados;
        boolean fechasCompletas = true;
        for (var episodio : episodios) {
            if (episodio == null) {
                datosCompletos = false;
                continue;
            }
            boolean unico = episodio.id() != null && episodio.id() > 0 && ids.add(episodio.id())
                    && episodio.numero() != null && episodio.numero() > 0
                    && episodio.numero() <= esperados && numeros.add(episodio.numero());
            datosCompletos &= unico;
            fechasCompletas &= episodio.estreno() != null;
            if (unico && episodio.estreno() != null && !episodio.estreno().isAfter(hoy)) {
                estrenados++;
            }
        }
        boolean disponible = datosCompletos && fechasCompletas && estrenados == esperados;
        String motivo = disponible ? null : !datosCompletos || !fechasCompletas
                ? "TMDB todavía no tiene información completa de esta temporada"
                : "Todavía quedan episodios por estrenar";
        return new DisponibilidadTemporadaResponse(serieId, TipoContenido.SERIE, resumen.numero(),
                esperados, estrenados, disponible, motivo);
    }

    private TmdbSerieDetalle detalleSerie(Long serieId) {
        TipoContenido.SERIE.clave(serieId);
        var serie = consultar("/tv/" + serieId, TmdbSerieDetalle.class);
        if (!Objects.equals(serie.id(), serieId) || serie.seasons() == null) {
            throw catalogoNoDisponible();
        }
        return serie;
    }

    private List<TmdbSerieDetalle.Temporada> temporadasRegulares(TmdbSerieDetalle serie) {
        return serie.seasons().stream().filter(Objects::nonNull)
                .filter(t -> t.numero() != null && t.numero() > 0).toList();
    }

    private <T> T consultar(String path, Class<T> tipo) {
        String url = UriComponentsBuilder.fromUriString(apiUrl).path(path)
                .queryParam("api_key", apiKey).queryParam("language", "es-ES")
                .build().encode().toUriString();
        try {
            T response = client.getForObject(url, tipo);
            if (response == null) throw catalogoNoDisponible();
            return response;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa serie o temporada");
            }
            throw catalogoNoDisponible();
        } catch (RestClientException ex) {
            // No devolver mensajes del cliente HTTP: pueden contener la URL con la clave TMDB.
            throw catalogoNoDisponible();
        }
    }

    private ResponseStatusException catalogoNoDisponible() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No pudimos consultar las temporadas. Probá de nuevo en un momento");
    }
}
