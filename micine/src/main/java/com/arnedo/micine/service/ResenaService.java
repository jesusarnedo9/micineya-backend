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
import java.util.stream.Collectors;
import com.arnedo.micine.dto.ResenaResponse;

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

    public void crearResena(String email, ResenaRequest request) {
        // 1. Buscamos al usuario que está escribiendo la reseña
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Buscamos la película o la creamos si no existe en nuestra BD
        Pelicula pelicula = peliculaRepository.findByTmdbId(request.getTmdbId()).orElse(null);

        if (pelicula == null) {
            Pelicula nuevaPeli = new Pelicula(request.getTmdbId(), request.getTitulo(), request.getPosterPath());
            pelicula = peliculaRepository.save(nuevaPeli);
        }

        // 3. Armamos la reseña uniendo todo
        Resena nuevaResena = new Resena(request.getCalificacion(), request.getComentario(), usuario, pelicula);

        // 4. Guardamos en la base de datos
        resenaRepository.save(nuevaResena);
    }

    public List obtenerResenasPorPelicula(Long tmdbId) {
        // 1. Buscamos todas las reseñas crudas
        List<Resena> resenasCrudas = resenaRepository.findByPeliculaTmdbId(tmdbId);

        // 2. Las convertimos a nuestro DTO limpio
        return resenasCrudas.stream()
                .map(r -> new ResenaResponse(
                        r.getCalificacion(),
                        r.getComentario(),
                        r.getUsuario().getEmail() // Solo exponemos el email, no el usuario entero
                ))
                .collect(Collectors.toList());
    }
}