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

    public ResenaService(ResenaRepository resenaRepository, UsuarioRepository usuarioRepository, PeliculaRepository peliculaRepository) {
        this.resenaRepository = resenaRepository;
        this.usuarioRepository = usuarioRepository;
        this.peliculaRepository = peliculaRepository;
    }

    @Transactional
    public ResenaResponse guardarResena(String email, ResenaRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(email)
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
        resena.setPelicula(pelicula);
        resena.setFechaActualizacion(LocalDateTime.now());

        return toResponse(resenaRepository.save(resena));
    }

    @Transactional(readOnly = true)
    public List<ResenaResponse> obtenerResenasPorPelicula(Long tmdbId) {
        return resenaRepository.findByPeliculaTmdbId(tmdbId).stream()
                .map(this::toResponse)
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
        resenas.forEach(resena ->
                ultimaPorPelicula.putIfAbsent(resena.getPelicula().getTmdbId(), resena));

        return ultimaPorPelicula.values().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void marcarComoNoVista(String email, Long tmdbId) {
        List<Resena> resenas = resenaRepository
                .findByUsuarioEmailAndPeliculaTmdbId(email, tmdbId);
        if (!resenas.isEmpty()) {
            resenaRepository.deleteAll(resenas);
        }
    }

    private ResenaResponse toResponse(Resena resena) {
        Pelicula pelicula = resena.getPelicula();
        return new ResenaResponse(
                resena.getId(),
                pelicula.getTmdbId(),
                pelicula.getTitulo(),
                pelicula.getPosterPath(),
                resena.getCalificacion(),
                resena.getComentario(),
                resena.getUsuario().getUsername(),
                resena.getFechaActualizacion()
        );
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim();
    }
}
