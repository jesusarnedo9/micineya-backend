package com.arnedo.micine.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "plataformas")
public class Plataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(unique = true)
    private Integer tmdbProviderId;

    // Constructor vacío obligatorio
    public Plataforma() {}

    public Plataforma(String nombre, Integer tmdbProviderId) {
        this.nombre = nombre;
        this.tmdbProviderId = tmdbProviderId;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getTmdbProviderId() { return tmdbProviderId; }
    public void setTmdbProviderId(Integer tmdbProviderId) { this.tmdbProviderId = tmdbProviderId; }
}
