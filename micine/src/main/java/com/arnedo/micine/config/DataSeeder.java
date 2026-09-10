package com.arnedo.micine.config;

import com.arnedo.micine.entity.Genero;
import com.arnedo.micine.entity.Plataforma;
import com.arnedo.micine.repository.GeneroRepository;
import com.arnedo.micine.repository.PlataformaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {

    private final GeneroRepository generoRepository;
    private final PlataformaRepository plataformaRepository;

    public DataSeeder(GeneroRepository generoRepository, PlataformaRepository plataformaRepository) {
        this.generoRepository = generoRepository;
        this.plataformaRepository = plataformaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        guardarOActualizarPlataforma("Netflix", 8);
        guardarOActualizarPlataforma("Max", 1899);
        guardarOActualizarPlataforma("Disney+", 337);
        guardarOActualizarPlataforma("Prime Video", 119);
        guardarOActualizarPlataforma("MUBI", 11);
        guardarOActualizarPlataforma("Apple TV+", 350);
        guardarOActualizarPlataforma("Paramount+", 531);
        guardarOActualizarPlataforma("Crunchyroll", 283);

        // Upsert: conserva los IDs y las preferencias de las cuentas existentes.
        guardarOActualizarGenero("Acción", 28, 10759);
        guardarOActualizarGenero("Comedia", 35, 35);
        guardarOActualizarGenero("Terror", 27, null);
        guardarOActualizarGenero("Ciencia Ficción", 878, 10765);
        guardarOActualizarGenero("Romance", 10749, null);
        guardarOActualizarGenero("Aventura", 12, 10759);
        guardarOActualizarGenero("Animación", 16, 16);
        guardarOActualizarGenero("Crimen", 80, 80);
        guardarOActualizarGenero("Documental", 99, 99);
        guardarOActualizarGenero("Drama", 18, 18);
        guardarOActualizarGenero("Familia", 10751, 10751);
        guardarOActualizarGenero("Fantasía", 14, 10765);
        guardarOActualizarGenero("Historia", 36, null);
        guardarOActualizarGenero("Misterio", 9648, 9648);
        guardarOActualizarGenero("Música", 10402, null);
        guardarOActualizarGenero("Suspenso", 53, null);
        guardarOActualizarGenero("Guerra", 10752, 10768);
        guardarOActualizarGenero("Western", 37, 37);
        guardarOActualizarGenero("Película de TV", 10770, null);
    }

    private void guardarOActualizarGenero(String nombre, Integer movieId, Integer tvId) {
        Genero genero = generoRepository.findByTmdbId(movieId)
                .or(() -> generoRepository.findByNombre(nombre))
                .orElseGet(() -> new Genero(nombre, movieId));
        genero.setTmdbId(movieId);
        genero.setTmdbTvId(tvId);
        generoRepository.save(genero);
    }

    private void guardarOActualizarPlataforma(String nombre, Integer tmdbProviderId) {
        Plataforma plataforma = plataformaRepository.findByNombre(nombre)
                .orElseGet(() -> new Plataforma(nombre, tmdbProviderId));
        plataforma.setTmdbProviderId(tmdbProviderId);
        plataformaRepository.save(plataforma);
    }
}
