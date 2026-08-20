package com.arnedo.micine.controller;

import com.arnedo.micine.entity.Genero;
import com.arnedo.micine.entity.Plataforma;
import com.arnedo.micine.repository.GeneroRepository;
import com.arnedo.micine.repository.PlataformaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final GeneroRepository generoRepository;
    private final PlataformaRepository plataformaRepository;

    public CatalogoController(GeneroRepository generoRepository, PlataformaRepository plataformaRepository) {
        this.generoRepository = generoRepository;
        this.plataformaRepository = plataformaRepository;
    }

    @GetMapping("/plataformas")
    public List getPlataformas() {
        return plataformaRepository.findAll();
    }

    @GetMapping("/generos")
    public List getGeneros() {
        return generoRepository.findAll();
    }
}