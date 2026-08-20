package com.arnedo.micine.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "peliculas")
public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El ID real de la película en la API de TMDB
    @Column(nullable = false, unique = true)
    private Long tmdbId;

    private String titulo;
    private String posterPath;

    public Pelicula() {}

    public Pelicula(Long tmdbId, String titulo, String posterPath) {
        this.tmdbId = tmdbId;
        this.titulo = titulo;
        this.posterPath = posterPath;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTmdbId() { return tmdbId; }
    public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
}