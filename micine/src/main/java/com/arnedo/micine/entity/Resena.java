package com.arnedo.micine.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resenas")
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardamos las estrellas (por ejemplo de 1 a 5)
    @Column(nullable = false)
    private Integer calificacion;

    // Le damos un poco más de espacio al texto del comentario (hasta 500 caracteres)
    @Column(length = 500)
    private String comentario;

    // Relación: Muchas reseñas pertenecen a UN usuario
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Relación: Muchas reseñas pertenecen a UNA película
    @ManyToOne
    @JoinColumn(name = "pelicula_id", nullable = false)
    private Pelicula pelicula;

    private LocalDateTime fechaActualizacion;

    public Resena() {}

    public Resena(Integer calificacion, String comentario, Usuario usuario, Pelicula pelicula) {
        this.calificacion = calificacion;
        this.comentario = comentario;
        this.usuario = usuario;
        this.pelicula = pelicula;
    }

    @PrePersist
    @PreUpdate
    private void actualizarFecha() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCalificacion() { return calificacion; }
    public void setCalificacion(Integer calificacion) { this.calificacion = calificacion; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Pelicula getPelicula() { return pelicula; }
    public void setPelicula(Pelicula pelicula) { this.pelicula = pelicula; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
