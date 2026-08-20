package com.arnedo.micine.dto;

import com.arnedo.micine.entity.Resena;

public class ResenaResponse {

    private Integer calificacion;
    private String comentario;
    private String autor; // Acá solo vamos a mandar el email del usuario

    public ResenaResponse(Integer calificacion, String comentario, String autor) {
        this.calificacion = calificacion;
        this.comentario = comentario;
        this.autor = autor;
    }

    // Getters y Setters
    public Integer getCalificacion() { return calificacion; }
    public void setCalificacion(Integer calificacion) { this.calificacion = calificacion; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
}