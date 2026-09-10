package com.arnedo.micine.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

/** El ID de TMDB es único solamente dentro de su catálogo. */
public enum TipoContenido {
    PELICULA("movie"), SERIE("tv");

    private final String codigo;

    TipoContenido(String codigo) { this.codigo = codigo; }

    @JsonValue
    public String getCodigo() { return codigo; }

    @JsonCreator
    public static TipoContenido desdeCodigo(String codigo) {
        for (var tipo : values()) if (tipo.codigo.equals(codigo)) return tipo;
        throw new IllegalArgumentException("El tipo debe ser movie o tv");
    }

    public String clave(Long tmdbId) {
        if (tmdbId == null || tmdbId <= 0) {
            throw new IllegalArgumentException("El contenido no es válido");
        }
        return codigo + ":" + tmdbId;
    }
}
