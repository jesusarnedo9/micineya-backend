package com.arnedo.micine.service;

import com.arnedo.micine.dto.CatalogoSeriesResponse;
import com.arnedo.micine.dto.PerfilRecomendacion;
import com.arnedo.micine.dto.TipoContenido;
import com.arnedo.micine.dto.TmdbResponse;
import com.arnedo.micine.entity.Genero;
import com.arnedo.micine.entity.Plataforma;
import com.arnedo.micine.repository.UsuarioRepository;
import com.arnedo.micine.repository.ResenaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CatalogoSeriesService {
    private final UsuarioRepository usuarios;
    private final TmdbService tmdb;
    private final TransactionTemplate lectura;
    private final TransactionTemplate escritura;
    private final ResenaRepository resenas;

    public CatalogoSeriesService(UsuarioRepository usuarios, TmdbService tmdb,
                                 PlatformTransactionManager transactions, ResenaRepository resenas) {
        this.usuarios = usuarios;
        this.tmdb = tmdb;
        this.lectura = new TransactionTemplate(transactions);
        this.lectura.setReadOnly(true);
        this.escritura = new TransactionTemplate(transactions);
        this.resenas = resenas;
    }

    public CatalogoSeriesResponse consultar(String email) {
        return recomendar(email, Set.of(), false);
    }

    public TmdbResponse buscar(String query) {
        try {
            return tmdb.buscarSeries(query);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No pudimos buscar series. Probá de nuevo en un momento");
        }
    }

    public CatalogoSeriesResponse recomendar(String email, Set<Long> actualesIds, boolean registrar) {
        // Materializar las preferencias antes de llamar a TMDB: no mantener una conexión
        // a la base de datos ocupada mientras se consultan catálogo y trailers.
        Preferencias preferencias = Objects.requireNonNull(lectura.execute(status -> {
            var usuario = usuarios.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            Set<Integer> generos = usuario.getGeneros().stream().map(Genero::getTmdbTvId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            Set<Integer> plataformas = usuario.getPlataformas().stream().map(Plataforma::getTmdbProviderId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            List<String> sinEquivalencia = usuario.getGeneros().stream()
                    .filter(g -> g.getTmdbTvId() == null).map(Genero::getNombre).sorted().toList();
            var ahora = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
            var vistas = resenas.findByUsuarioIdAndPeliculaMediaType(usuario.getId(), TipoContenido.SERIE)
                    .stream().filter(r -> !r.getTemporadasVistas().isEmpty())
                    .map(r -> r.getPelicula().getTmdbId()).collect(Collectors.toSet());
            var favoritas = usuario.getPeliculasFavoritas().stream()
                    .filter(p -> p.getMediaType() == TipoContenido.SERIE)
                    .map(com.arnedo.micine.entity.Pelicula::getTmdbId).collect(Collectors.toSet());
            var descartadas = usuario.getSeriesDescartadas().entrySet().stream()
                    .filter(e -> e.getValue().isAfter(ahora)).map(java.util.Map.Entry::getKey).collect(Collectors.toSet());
            return new Preferencias(generos, plataformas, sinEquivalencia, vistas, favoritas,
                    descartadas, Set.copyOf(usuario.getSeriesRecientes().keySet()));
        }));

        // No ampliar silenciosamente una selección que solo contiene géneros de películas.
        if (preferencias.generos().isEmpty() || preferencias.plataformas().isEmpty()) {
            return new CatalogoSeriesResponse(List.of(), preferencias.sinEquivalencia());
        }
        // Los IDs de películas NO se reutilizan como vistas, favoritas ni descartadas de TV.
        var perfil = new PerfilRecomendacion(preferencias.generos(), preferencias.plataformas(),
                preferencias.vistas(), preferencias.favoritas(), preferencias.descartadas(), preferencias.recientes());
        try {
            var resultado = tmdb.getRecomendaciones(perfil, actualesIds, TipoContenido.SERIE);
            if (registrar) registrar(email, resultado.getResults().stream().map(com.arnedo.micine.dto.PeliculaDto::getId).toList());
            return new CatalogoSeriesResponse(resultado.getResults(), preferencias.sinEquivalencia());
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No pudimos consultar las series. Probá de nuevo en un momento");
        }
    }

    private void registrar(String email, List<Long> ids) {
        escritura.executeWithoutResult(status -> {
            var usuario = usuarios.findByEmailForUpdate(email).orElseThrow();
            var ahora = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
            var recientes = usuario.getSeriesRecientes();
            ids.forEach(id -> recientes.put(id, ahora));
            recientes.entrySet().stream()
                    .sorted(java.util.Map.Entry.<Long, java.time.LocalDateTime>comparingByValue().reversed()
                            .thenComparing(java.util.Map.Entry.comparingByKey()))
                    .skip(50).map(java.util.Map.Entry::getKey).toList().forEach(recientes::remove);
            usuario.getSeriesDescartadas().entrySet().removeIf(e -> !e.getValue().isAfter(ahora));
        });
    }

    public void descartar(String email, Long id, boolean descartada) {
        TipoContenido.SERIE.clave(id);
        escritura.executeWithoutResult(status -> {
            var usuario = usuarios.findByEmailForUpdate(email).orElseThrow();
            var ahora = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
            usuario.getSeriesDescartadas().entrySet().removeIf(e -> !e.getValue().isAfter(ahora));
            if (descartada) usuario.getSeriesDescartadas().put(id, ahora.plusDays(30));
            else usuario.getSeriesDescartadas().remove(id);
        });
    }

    private record Preferencias(Set<Integer> generos, Set<Integer> plataformas, List<String> sinEquivalencia,
            Set<Long> vistas, Set<Long> favoritas, Set<Long> descartadas, Set<Long> recientes) {}
}
