package com.arnedo.micine.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "generos")
public class Genero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    // El ID oficial de TMDB (ej. 28 para Acción, 35 para Comedia)
    @Column(unique = true)
    private Integer tmdbId;

    public Genero() {}

    public Genero(String nombre, Integer tmdbId) {
        this.nombre = nombre;
        this.tmdbId = tmdbId;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }
}