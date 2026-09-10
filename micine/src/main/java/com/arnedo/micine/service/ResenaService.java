package com.arnedo.micine.service;

import com.arnedo.micine.dto.ResenaRequest;
import com.arnedo.micine.entity.Pelicula;
import com.arnedo.micine.entity.Resena;
import com.arnedo.micine.entity.Usuario;
import com.arnedo.micine.repository.PeliculaRepository;
import com.arnedo.micine.repository.ResenaRepository;
import com.arnedo.micine.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import com.arnedo.micine.dto.ResenaResponse;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResenaService {

    private final ResenaRepository resenaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PeliculaRepository peliculaRepository;
    private final ComunidadService comunidad;
    private final com.arnedo.micine.repository.ReporteComunidadRepository reportes;

    public ResenaService(ResenaRepository resenaRepository, UsuarioRepository usuarioRepository, PeliculaRepository peliculaRepository,
                         ComunidadService comunidad, com.arnedo.micine.repository.ReporteComunidadRepository reportes) {
        this.resenaRepository = resenaRepository;
        this.usuarioRepository = usuarioRepository;
        this.peliculaRepository = peliculaRepository;
        this.comunidad = comunidad;
        this.reportes = reportes;
    }

    @Transactional
    public ResenaResponse guardarResena(String email, ResenaRequest request) {
        Usuario usuario = usuarioRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Pelicula pelicula = peliculaRepository.findByTmdbId(request.getTmdbId())
                .orElseGet(() -> peliculaRepository.save(new Pelicula(
                        request.getTmdbId(), limpiar(request.getTitulo()), request.getPosterPath())));

        pelicula.setTitulo(limpiar(request.getTitulo()));
        pelicula.setPosterPath(request.getPosterPath());

        Resena resena = resenaRepository
                .findFirstByUsuarioEmailAndPeliculaTmdbIdOrderByIdDesc(email, request.getTmdbId())
                .orElseGet(() -> new Resena(
                        request.getCalificacion(), limpiar(request.getComentario()), usuario, pelicula));

        resena.setCalificacion(request.getCalificacion());
        resena.setComentario(limpiar(request.getComentario()));
        resena.setSpoiler(request.isSpoiler());
        resena.setPelicula(pelicula);
        resena.setFechaVista(resena.getFechaVista());
        resena.setFechaActualizacion(LocalDateTime.now());

        return toResponse(resenaRepository.save(resena));
    }

    @Transactional(readOnly = true)
    public List<ResenaResponse> obtenerResenasPorPelicula(String email, Long tmdbId) {
        Usuario yo = usuarioRepository.findByEmail(email).orElseThrow();
        return resenaRepository.findByPeliculaTmdbId(tmdbId).stream()
                .filter(resena -> comunidad.puedeVerResena(yo, resena))
                .map(ResenaService::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResenaResponse> obtenerMisResenas(String email) {
        LinkedHashMap<Long, Resena> ultimaPorPelicula = new LinkedHashMap<>();
        List<Resena> resenas = resenaRepository.findByUsuarioEmail(email);
        resenas.sort(Comparator
                .comparing(
                        Resena::getFechaActualizacion,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Resena::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        resenas.stream().filter(r -> r.getPelicula().getMediaType() == com.arnedo.micine.dto.TipoContenido.PELICULA).forEach(resena ->
                ultimaPorPelicula.putIfAbsent(resena.getPelicula().getTmdbId(), resena));

        return ultimaPorPelicula.values().stream()
                .map(ResenaService::toResponse)
                .toList();
    }

    @Transactional
    public void marcarComoNoVista(String email, Long tmdbId) {
        usuarioRepository.findByEmailForUpdate(email).orElseThrow();
        List<Resena> resenas = resenaRepository
                .findByUsuarioEmailAndPeliculaTmdbId(email, tmdbId);
        if (!resenas.isEmpty()) {
            reportes.deleteByResenaIdIn(resenas.stream().map(Resena::getId).toList());
            resenaRepository.deleteAll(resenas);
        }
    }

    public static ResenaResponse toResponse(Resena resena) {
        Pelicula pelicula = resena.getPelicula();
        return new ResenaResponse(
                resena.getId(),
                pelicula.getTmdbId(),
                pelicula.getTitulo(),
                pelicula.getPosterPath(),
                resena.getCalificacion(),
                resena.getComentario(),
                resena.getUsuario().getUsername(),
                resena.getFechaActualizacion(),
                resena.isSpoiler(),
                resena.isOcultadaModeracion(),
                pelicula.getMediaType(),
                java.util.Set.copyOf(resena.getTemporadasVistas()),
                resena.getFechaVista()
        );
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim();
    }
}
