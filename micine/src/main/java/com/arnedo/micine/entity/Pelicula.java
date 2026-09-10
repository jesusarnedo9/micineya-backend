package com.arnedo.micine.entity;

import jakarta.persistence.*;
import com.arnedo.micine.dto.TipoContenido;

@Entity
@Table(name = "peliculas", uniqueConstraints = @UniqueConstraint(name = "uk_contenido_tipo_tmdb", columnNames = {"media_type", "tmdb_id"}))
public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El ID real de la película en la API de TMDB
    @Column(nullable = false)
    private Long tmdbId;

    // Nombre de entidad/tabla conservado para no romper las FK existentes.
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, columnDefinition = "varchar(16) default 'PELICULA'")
    private TipoContenido mediaType = TipoContenido.PELICULA;

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
    public TipoContenido getMediaType() { return mediaType; }
    public void setMediaType(TipoContenido mediaType) { this.mediaType = mediaType; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
}
