package com.arnedo.micine.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "token_version")
    private Integer tokenVersion = 0;

    // Las cuentas anteriores conservan null; todas las nuevas exigen su ID en el JWT.
    @Column(name = "requiere_token_con_id")
    private Boolean requiereTokenConId = true;

    public boolean requiereTokenConId() { return Boolean.TRUE.equals(requiereTokenConId); }

    private LocalDateTime fechaCreacion;

    private String normasComunidadVersion;
    private Boolean comunidadSuspendida = false;

    public String getNormasComunidadVersion() { return normasComunidadVersion; }
    public void setNormasComunidadVersion(String version) { normasComunidadVersion = version; }
    public boolean isComunidadSuspendida() { return Boolean.TRUE.equals(comunidadSuspendida); }
    public void setComunidadSuspendida(boolean value) { comunidadSuspendida = value; }

    @ManyToMany
    @JoinTable(
            name = "usuario_plataforma",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "plataforma_id")
    )
    private Set<Plataforma> plataformas = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "usuario_genero",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "genero_id")
    )
    private Set<Genero> generos = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "usuario_pelicula_favorita",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "pelicula_id")
    )
    private Set<Pelicula> peliculasFavoritas = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "usuario_recomendacion_reciente", joinColumns = @JoinColumn(name = "usuario_id"))
    @MapKeyColumn(name = "tmdb_id")
    @Column(name = "mostrada_en", nullable = false)
    private Map<Long, LocalDateTime> recomendacionesRecientes = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "usuario_pelicula_descartada", joinColumns = @JoinColumn(name = "usuario_id"))
    @MapKeyColumn(name = "tmdb_id")
    @Column(name = "hasta", nullable = false)
    private Map<Long, LocalDateTime> peliculasDescartadas = new HashMap<>();

    public Map<Long, LocalDateTime> getRecomendacionesRecientes() { return recomendacionesRecientes; }
    public Map<Long, LocalDateTime> getPeliculasDescartadas() { return peliculasDescartadas; }

    @ElementCollection
    @CollectionTable(name = "usuario_serie_reciente", joinColumns = @JoinColumn(name = "usuario_id"))
    @MapKeyColumn(name = "tmdb_id")
    @Column(name = "mostrada_en", nullable = false)
    private Map<Long, LocalDateTime> seriesRecientes = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "usuario_serie_descartada", joinColumns = @JoinColumn(name = "usuario_id"))
    @MapKeyColumn(name = "tmdb_id")
    @Column(name = "hasta", nullable = false)
    private Map<Long, LocalDateTime> seriesDescartadas = new HashMap<>();

    public Map<Long, LocalDateTime> getSeriesRecientes() { return seriesRecientes; }
    public Map<Long, LocalDateTime> getSeriesDescartadas() { return seriesDescartadas; }

    // Constructor vacío (obligatorio para Spring Boot)
    public Usuario() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getTokenVersion() { return tokenVersion == null ? 0 : tokenVersion; }
    public void setTokenVersion(Integer tokenVersion) { this.tokenVersion = tokenVersion; }
    public void incrementarTokenVersion() { this.tokenVersion = getTokenVersion() + 1; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public Set<Plataforma> getPlataformas() { return plataformas; }
    public void setPlataformas(Set<Plataforma> plataformas) { this.plataformas = plataformas; }

    public Set<Genero> getGeneros() { return generos; }
    public void setGeneros(Set<Genero> generos) { this.generos = generos; }

    public Set<Pelicula> getPeliculasFavoritas() { return peliculasFavoritas; }
    public void setPeliculasFavoritas(Set<Pelicula> peliculasFavoritas) { this.peliculasFavoritas = peliculasFavoritas; }
}
