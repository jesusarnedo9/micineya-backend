package com.arnedo.micine.service;

import com.arnedo.micine.dto.AuthResponse;
import com.arnedo.micine.dto.LoginRequest;
import com.arnedo.micine.dto.OnboardingRequest;
import com.arnedo.micine.dto.RegistroRequest;
import com.arnedo.micine.entity.Usuario;
import com.arnedo.micine.repository.UsuarioRepository;
import com.arnedo.micine.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.arnedo.micine.entity.Genero;
import com.arnedo.micine.entity.Plataforma;
import com.arnedo.micine.repository.GeneroRepository;
import com.arnedo.micine.repository.PlataformaRepository;
import com.arnedo.micine.entity.Pelicula;
import com.arnedo.micine.repository.PeliculaRepository;
import com.arnedo.micine.dto.PeliculaRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PlataformaRepository plataformaRepository;
    private final GeneroRepository generoRepository;
    private final PeliculaRepository peliculaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          PlataformaRepository plataformaRepository,
                          GeneroRepository generoRepository,
                          PeliculaRepository peliculaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.plataformaRepository = plataformaRepository;
        this.generoRepository = generoRepository;
        this.peliculaRepository = peliculaRepository;
    }

    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ese email ya está registrado");
        }
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Ese username ya está en uso");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuarioRepository.save(usuario);

        String token = jwtService.generarToken(usuario.getEmail());
        return new AuthResponse(token, usuario.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        String token = jwtService.generarToken(usuario.getEmail());
        return new AuthResponse(token, usuario.getUsername());
    }

    public void guardarPreferencias(String email, OnboardingRequest request) {
        // 1. Buscamos al usuario
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Buscamos las plataformas y géneros por los IDs que mandó el celular
        List plataformas = plataformaRepository.findAllById(request.getPlataformaIds());
        List generos = generoRepository.findAllById(request.getGeneroIds());

        // 3. Se los asignamos al usuario (transformando la List a Set)
        usuario.setPlataformas(new HashSet<>(plataformas));
        usuario.setGeneros(new HashSet<>(generos));

        // 4. Guardamos los cambios
        usuarioRepository.save(usuario);
    }
    public String getGenerosTmdbIds(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Extraemos los tmdbId, los convertimos a texto y los unimos con comas
        return usuario.getGeneros().stream()
                .map(g -> String.valueOf(g.getTmdbId()))
                .collect(Collectors.joining(","));
    }

    public void agregarPeliculaFavorita(String email, PeliculaRequest request) {
        // 1. Buscamos al usuario logueado
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Buscamos si la película ya existe en nuestra BD. Si no existe, la creamos.
        Pelicula pelicula = peliculaRepository.findByTmdbId(request.getTmdbId())
                .orElseGet(() -> {
                    Pelicula nuevaPeli = new Pelicula(request.getTmdbId(), request.getTitulo(), request.getPosterPath());
                    return peliculaRepository.save(nuevaPeli);
                });

        // 3. Agregamos la película a la lista del usuario
        usuario.getPeliculasFavoritas().add(pelicula);

        // 4. Guardamos los cambios en el usuario
        usuarioRepository.save(usuario);
    }

    public Set getPeliculasFavoritas(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return usuario.getPeliculasFavoritas();
    }

    public void eliminarPeliculaFavorita(String email, Long tmdbId) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        usuario.getPeliculasFavoritas().removeIf(pelicula -> pelicula.getTmdbId().equals(tmdbId));

        usuarioRepository.save(usuario);
    }

}