package com.arnedo.micine.service;

import com.arnedo.micine.dto.*;
import com.arnedo.micine.entity.*;
import com.arnedo.micine.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BibliotecaService {
    private final UsuarioRepository usuarios;
    private final PeliculaRepository peliculas;
    private final ResenaRepository resenas;
    private final ReporteComunidadRepository reportes;
    private final ContenidoService contenidos;
    private final TemporadasService temporadas;
    private final TransactionTemplate lectura;
    private final TransactionTemplate escritura;

    public BibliotecaService(UsuarioRepository usuarios, PeliculaRepository peliculas, ResenaRepository resenas,
            ReporteComunidadRepository reportes, ContenidoService contenidos, TemporadasService temporadas,
            PlatformTransactionManager manager) {
        this.usuarios = usuarios; this.peliculas = peliculas; this.resenas = resenas;
        this.reportes = reportes; this.contenidos = contenidos; this.temporadas = temporadas;
        lectura = new TransactionTemplate(manager); lectura.setReadOnly(true);
        escritura = new TransactionTemplate(manager);
    }

    public List<BibliotecaDtos.Favorita> favoritas(String email) {
        return lectura.execute(status -> usuarios.findByEmail(email).orElseThrow().getPeliculasFavoritas().stream()
                .sorted(Comparator.comparing(Pelicula::getTitulo, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(BibliotecaService::favorita).toList());
    }

    public BibliotecaDtos.Favorita guardar(String email, BibliotecaDtos.Guardar request) {
        validarIdentidad(request.mediaType(), request.tmdbId());
        String titulo = request.titulo(); String poster = request.posterPath();
        if (request.mediaType() == TipoContenido.SERIE) {
            var existente = peliculas.findByMediaTypeAndTmdbId(TipoContenido.SERIE, request.tmdbId());
            if (existente.isEmpty()) {
                var serie = temporadas.listar(request.tmdbId());
                titulo = serie.titulo(); poster = serie.posterPath();
            }
        }
        var contenido = contenidos.obtenerOCrear(request.mediaType(), request.tmdbId(), titulo, poster);
        return escritura.execute(status -> {
            var usuario = usuarios.findByEmailForUpdate(email).orElseThrow();
            // Comparar IDs, no identidad Java de una entidad que pudo venir de otra transacción.
            if (usuario.getPeliculasFavoritas().stream().noneMatch(p -> p.getId().equals(contenido.getId()))) {
                usuario.getPeliculasFavoritas().add(peliculas.getReferenceById(contenido.getId()));
            }
            return favorita(contenido);
        });
    }

    public void quitarGuardada(String email, TipoContenido tipo, Long id) {
        validarIdentidad(tipo, id);
        escritura.executeWithoutResult(status -> usuarios.findByEmailForUpdate(email).orElseThrow()
                .getPeliculasFavoritas().removeIf(p -> p.getMediaType() == tipo && p.getTmdbId().equals(id)));
    }

    public List<ResenaResponse> mias(String email) {
        return lectura.execute(status -> {
            // Una tarjeta por contenido, conservando compatibilidad con duplicados históricos de películas.
            var ultimas = new LinkedHashMap<Long, Resena>();
            resenas.findByUsuarioEmail(email).stream().sorted(Comparator.comparing(Resena::getId).reversed())
                    .forEach(r -> ultimas.putIfAbsent(r.getPelicula().getId(), r));
            return ultimas.values().stream().sorted(Comparator
                            .comparing(Resena::getFechaVista, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Resena::getId, Comparator.reverseOrder()))
                    .map(ResenaService::toResponse).toList();
        });
    }

    public ResenaResponse resenar(String email, BibliotecaDtos.Resenar request) {
        validarIdentidad(request.mediaType(), request.tmdbId());
        var seleccion = request.temporadasVistas();
        if (request.calificacion() == null || request.calificacion() < 1 || request.calificacion() > 5
                || request.comentario() != null && request.comentario().length() > 500
                || seleccion == null || seleccion.size() > 100
                || seleccion.stream().anyMatch(n -> n == null || n <= 0)) {
            throw new IllegalArgumentException("La reseña o sus temporadas no son válidas");
        }
        if (request.mediaType() == TipoContenido.SERIE && seleccion.isEmpty()) {
            throw new IllegalArgumentException("Elegí al menos una temporada completa o marcá la serie como no vista");
        }
        if (request.mediaType() == TipoContenido.PELICULA && !seleccion.isEmpty()) {
            throw new IllegalArgumentException("Las películas no tienen temporadas");
        }
        var anteriores = Objects.requireNonNull(lectura.execute(status -> buscar(email, request.mediaType(), request.tmdbId())
                .stream().findFirst().map(r -> Set.copyOf(r.getTemporadasVistas())).orElse(Set.of())));
        var nuevas = new HashSet<>(seleccion); nuevas.removeAll(anteriores);
        String titulo = request.titulo(); String poster = request.posterPath();
        // Solo verificar las temporadas agregadas. Editar el texto o quitar una temporada no depende de TMDB.
        // Ninguna consulta HTTP se hace mientras se mantiene el bloqueo de la cuenta.
        if (request.mediaType() == TipoContenido.SERIE && !nuevas.isEmpty()) {
            var serie = temporadas.validarParaRegistro(request.tmdbId(), nuevas);
            titulo = serie.titulo(); poster = serie.posterPath();
        }
        var contenido = contenidos.obtenerOCrear(request.mediaType(), request.tmdbId(), titulo, poster);
        return escritura.execute(status -> {
            var usuario = usuarios.findByEmailForUpdate(email).orElseThrow();
            var anterioresBloqueadas = buscar(email, request.mediaType(), request.tmdbId());
            var resena = anterioresBloqueadas.stream().findFirst()
                    .orElseGet(() -> new Resena(request.calificacion(), "", usuario, peliculas.getReferenceById(contenido.getId())));
            LocalDateTime fecha = resena.getFechaVista();
            boolean sumaTemporada = !resena.getTemporadasVistas().containsAll(seleccion);
            resena.setFechaVista(fecha == null || sumaTemporada ? LocalDateTime.now() : fecha);
            resena.setCalificacion(request.calificacion());
            resena.setComentario(request.comentario() == null ? "" : request.comentario().trim());
            resena.setSpoiler(request.spoiler());
            resena.getTemporadasVistas().clear(); resena.getTemporadasVistas().addAll(seleccion);
            // Consolidar duplicados antiguos de este usuario y contenido, nunca de otro tipo o cuenta.
            eliminar(anterioresBloqueadas.stream().skip(1).toList());
            return ResenaService.toResponse(resenas.saveAndFlush(resena));
        });
    }

    public void marcarNoVista(String email, TipoContenido tipo, Long id) {
        validarIdentidad(tipo, id);
        escritura.executeWithoutResult(status -> {
            usuarios.findByEmailForUpdate(email).orElseThrow();
            eliminar(buscar(email, tipo, id));
        });
    }

    private List<Resena> buscar(String email, TipoContenido tipo, Long id) {
        return resenas.findByUsuarioEmailAndPeliculaMediaTypeAndPeliculaTmdbIdOrderByIdDesc(email, tipo, id);
    }
    private void eliminar(List<Resena> lista) {
        if (lista.isEmpty()) return;
        reportes.deleteByResenaIdIn(lista.stream().map(Resena::getId).toList());
        resenas.deleteAll(lista);
    }
    private static void validarIdentidad(TipoContenido tipo, Long id) {
        if (tipo == null) throw new IllegalArgumentException("Indicá si es película o serie");
        tipo.clave(id);
    }
    private static BibliotecaDtos.Favorita favorita(Pelicula p) {
        return new BibliotecaDtos.Favorita(p.getId(), p.getMediaType(), p.getTmdbId(), p.getTitulo(), p.getPosterPath());
    }
}
