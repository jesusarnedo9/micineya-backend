package com.arnedo.micine.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

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

    private Boolean spoiler = false;
    private Boolean ocultadaModeracion = false;

    public boolean isSpoiler() { return Boolean.TRUE.equals(spoiler); }
    public void setSpoiler(boolean value) { spoiler = value; }
    public boolean isOcultadaModeracion() { return Boolean.TRUE.equals(ocultadaModeracion); }
    public void setOcultadaModeracion(boolean value) { ocultadaModeracion = value; }

    // Relación: Muchas reseñas pertenecen a UN usuario
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Relación: Muchas reseñas pertenecen a UNA película
    @ManyToOne
    @JoinColumn(name = "pelicula_id", nullable = false)
    private Pelicula pelicula;

    private LocalDateTime fechaActualizacion;
    private LocalDateTime fechaVista;

    // En películas queda null. En series identifica la temporada puntuada por esta reseña.
    // Es nullable para poder migrar sin perder las reseñas de series creadas antes de esta versión.
    private Integer numeroTemporada;

    @ElementCollection
    @CollectionTable(name = "resena_temporada_vista", joinColumns = @JoinColumn(name = "resena_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"resena_id", "numero"}))
    @Column(name = "numero", nullable = false)
    private Set<Integer> temporadasVistas = new HashSet<>();

    public Set<Integer> getTemporadasVistas() { return temporadasVistas; }
    public Integer getNumeroTemporada() { return numeroTemporada; }
    public void setNumeroTemporada(Integer value) { numeroTemporada = value; }
    public LocalDateTime getFechaVista() { return fechaVista == null ? fechaActualizacion : fechaVista; }
    public void setFechaVista(LocalDateTime value) { fechaVista = value; }

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
        if (fechaVista == null) fechaVista = fechaActualizacion == null ? LocalDateTime.now() : fechaActualizacion;
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
