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
            // Películas: una tarjeta por contenido. Series: una tarjeta y reseña por temporada.
            var ultimas = new LinkedHashMap<String, Resena>();
            resenas.findByUsuarioEmail(email).stream().sorted(Comparator.comparing(Resena::getId).reversed())
                    .forEach(r -> ultimas.putIfAbsent(claveResena(r), r));
            return ultimas.values().stream().sorted(Comparator
                            .comparing(Resena::getFechaVista, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Resena::getId, Comparator.reverseOrder()))
                    .flatMap(r -> respuestasCompatibles(r).stream()).toList();
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
        if (request.mediaType() == TipoContenido.SERIE && seleccion.size() != 1) {
            throw new IllegalArgumentException("Elegí una temporada completa para puntuarla");
        }
        if (request.mediaType() == TipoContenido.PELICULA && !seleccion.isEmpty()) {
            throw new IllegalArgumentException("Las películas no tienen temporadas");
        }
        var anteriores = Objects.requireNonNull(lectura.execute(status -> buscar(email, request.mediaType(), request.tmdbId())
                .stream().flatMap(r -> r.getTemporadasVistas().stream()).collect(java.util.stream.Collectors.toSet())));
        var nuevas = new HashSet<>(seleccion); nuevas.removeAll(anteriores);
        String titulo = request.titulo(); String poster = request.posterPath();
        TemporadasSerieResponse catalogo = null;
        // Solo verificar las temporadas agregadas. Editar el texto o quitar una temporada no depende de TMDB.
        // Ninguna consulta HTTP se hace mientras se mantiene el bloqueo de la cuenta.
        if (request.mediaType() == TipoContenido.SERIE && !nuevas.isEmpty()) {
            catalogo = temporadas.validarParaRegistro(request.tmdbId(), nuevas);
            titulo = catalogo.titulo(); poster = catalogo.posterPath();
        }
        var requeridas = catalogo == null ? Set.<Integer>of() : temporadasEstrenadas(catalogo);
        var contenido = contenidos.obtenerOCrear(request.mediaType(), request.tmdbId(), titulo, poster);
        return escritura.execute(status -> {
            var usuario = usuarios.findByEmailForUpdate(email).orElseThrow();
            var anterioresBloqueadas = buscar(email, request.mediaType(), request.tmdbId());
            if (request.mediaType() == TipoContenido.SERIE) normalizarResenasDeSerie(anterioresBloqueadas);
            Integer numero = request.mediaType() == TipoContenido.SERIE ? seleccion.iterator().next() : null;
            var candidatas = anterioresBloqueadas.stream()
                    .filter(r -> request.mediaType() == TipoContenido.PELICULA || Objects.equals(r.getNumeroTemporada(), numero))
                    .sorted(Comparator.comparing(Resena::getId).reversed()).toList();
            var resena = candidatas.stream().findFirst()
                    .orElseGet(() -> new Resena(request.calificacion(), "", usuario, peliculas.getReferenceById(contenido.getId())));
            LocalDateTime fecha = resena.getFechaVista();
            resena.setFechaVista(fecha == null ? LocalDateTime.now() : fecha);
            resena.setCalificacion(request.calificacion());
            resena.setComentario(request.comentario() == null ? "" : request.comentario().trim());
            resena.setSpoiler(request.spoiler());
            resena.setNumeroTemporada(numero);
            resena.getTemporadasVistas().clear(); resena.getTemporadasVistas().addAll(seleccion);
            // Consolidar únicamente duplicados de la misma película o temporada.
            eliminar(candidatas.stream().skip(1).toList());
            var guardada = resenas.saveAndFlush(resena);
            var vistas = buscar(email, request.mediaType(), request.tmdbId()).stream()
                    .flatMap(r -> r.getTemporadasVistas().stream()).collect(java.util.stream.Collectors.toSet());
            boolean completa = request.mediaType() == TipoContenido.SERIE && !requeridas.isEmpty() && vistas.containsAll(requeridas);
            if (request.mediaType() == TipoContenido.PELICULA || completa) {
                usuario.getPeliculasFavoritas().removeIf(p -> p.getMediaType() == request.mediaType()
                        && p.getTmdbId().equals(request.tmdbId()));
            }
            return respuesta(guardada, completa);
        });
    }

    public void marcarTemporadaNoVista(String email, Long id, int numero) {
        TipoContenido.SERIE.clave(id);
        if (numero <= 0) throw new IllegalArgumentException("La temporada no es válida");
        escritura.executeWithoutResult(status -> {
            var usuario = usuarios.findByEmailForUpdate(email).orElseThrow();
            var existentes = buscar(email, TipoContenido.SERIE, id);
            normalizarResenasDeSerie(existentes);
            eliminar(existentes.stream().filter(r -> Objects.equals(r.getNumeroTemporada(), numero)).toList());
            peliculas.findByMediaTypeAndTmdbId(TipoContenido.SERIE, id).ifPresent(contenido -> {
                if (usuario.getPeliculasFavoritas().stream().noneMatch(p -> p.getId().equals(contenido.getId()))) {
                    usuario.getPeliculasFavoritas().add(peliculas.getReferenceById(contenido.getId()));
                }
            });
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
    private void normalizarResenasDeSerie(List<Resena> lista) {
        for (var original : List.copyOf(lista)) {
            if (original.getNumeroTemporada() != null || original.getTemporadasVistas().isEmpty()) continue;
            var numeros = original.getTemporadasVistas().stream().sorted().toList();
            original.setNumeroTemporada(numeros.get(0));
            original.getTemporadasVistas().clear(); original.getTemporadasVistas().add(numeros.get(0));
            for (int i = 1; i < numeros.size(); i++) {
                var copia = new Resena(original.getCalificacion(), original.getComentario(), original.getUsuario(), original.getPelicula());
                copia.setSpoiler(original.isSpoiler()); copia.setFechaVista(original.getFechaVista());
                copia.setNumeroTemporada(numeros.get(i)); copia.getTemporadasVistas().add(numeros.get(i));
                lista.add(resenas.save(copia));
            }
        }
        resenas.flush();
    }
    private static String claveResena(Resena r) {
        if (r.getPelicula().getMediaType() == TipoContenido.PELICULA) return "movie:" + r.getPelicula().getId();
        Integer numero = r.getNumeroTemporada();
        if (numero == null && r.getTemporadasVistas().size() == 1) numero = r.getTemporadasVistas().iterator().next();
        return "tv:" + r.getPelicula().getId() + ":" + (numero == null ? "legacy:" + r.getId() : numero);
    }
    private static Set<Integer> temporadasEstrenadas(TemporadasSerieResponse catalogo) {
        var hoy = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        return catalogo.temporadas().stream()
                .filter(t -> t.cantidadEpisodios() != null && t.cantidadEpisodios() > 0)
                .filter(t -> t.estreno() != null && !t.estreno().isAfter(hoy))
                .map(TemporadasSerieResponse.Temporada::numero).collect(java.util.stream.Collectors.toSet());
    }
    private static ResenaResponse respuesta(Resena r, boolean completa) {
        var base = ResenaService.toResponse(r);
        return new ResenaResponse(base.id(), base.tmdbId(), base.titulo(), base.posterPath(), base.calificacion(),
                base.comentario(), base.autor(), base.fechaActualizacion(), base.spoiler(), base.ocultadaModeracion(),
                base.mediaType(), base.temporadasVistas(), base.fechaVista(), base.numeroTemporada(), completa);
    }
    private static List<ResenaResponse> respuestasCompatibles(Resena r) {
        var base = ResenaService.toResponse(r);
        if (base.mediaType() != TipoContenido.SERIE || base.numeroTemporada() != null || base.temporadasVistas().size() < 2) {
            return List.of(base);
        }
        return base.temporadasVistas().stream().sorted().map(numero -> new ResenaResponse(base.id(), base.tmdbId(),
                base.titulo(), base.posterPath(), base.calificacion(), base.comentario(), base.autor(),
                base.fechaActualizacion(), base.spoiler(), base.ocultadaModeracion(), base.mediaType(), Set.of(numero),
                base.fechaVista(), numero, false)).toList();
    }
    private static void validarIdentidad(TipoContenido tipo, Long id) {
        if (tipo == null) throw new IllegalArgumentException("Indicá si es película o serie");
        tipo.clave(id);
    }
    private static BibliotecaDtos.Favorita favorita(Pelicula p) {
        return new BibliotecaDtos.Favorita(p.getId(), p.getMediaType(), p.getTmdbId(), p.getTitulo(), p.getPosterPath());
    }
}
