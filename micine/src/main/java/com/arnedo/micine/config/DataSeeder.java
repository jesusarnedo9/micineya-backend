package com.arnedo.micine.config;

import com.arnedo.micine.entity.Genero;
import com.arnedo.micine.entity.Plataforma;
import com.arnedo.micine.repository.GeneroRepository;
import com.arnedo.micine.repository.PlataformaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final GeneroRepository generoRepository;
    private final PlataformaRepository plataformaRepository;

    public DataSeeder(GeneroRepository generoRepository, PlataformaRepository plataformaRepository) {
        this.generoRepository = generoRepository;
        this.plataformaRepository = plataformaRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Si no hay plataformas, las creamos
        if (plataformaRepository.count() == 0) {
            plataformaRepository.saveAll(List.of(
                    new Plataforma("Netflix"),
                    new Plataforma("Max"),
                    new Plataforma("Disney+"),
                    new Plataforma("Prime Video")
            ));
            System.out.println("Plataformas iniciales cargadas.");
        }

        // Si no hay géneros, los creamos (con los IDs reales de la API de TMDB)
        if (generoRepository.count() == 0) {
            generoRepository.saveAll(List.of(
                    new Genero("Acción", 28),
                    new Genero("Comedia", 35),
                    new Genero("Terror", 27),
                    new Genero("Ciencia Ficción", 878),
                    new Genero("Romance", 10749)
            ));
            System.out.println("Géneros iniciales cargados.");
        }
    }
}