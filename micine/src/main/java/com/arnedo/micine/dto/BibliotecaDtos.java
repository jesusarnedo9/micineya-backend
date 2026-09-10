package com.arnedo.micine.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

public final class BibliotecaDtos {
    private BibliotecaDtos() {}

    public record Guardar(@NotNull TipoContenido mediaType, @NotNull @Positive Long tmdbId,
                           @Size(max = 255) String titulo, @Size(max = 255) String posterPath) {}
    public record Favorita(Long id, TipoContenido mediaType, Long tmdbId, String titulo, String posterPath) {}
    public record Resenar(@NotNull TipoContenido mediaType, @NotNull @Positive Long tmdbId,
                           @Size(max = 255) String titulo, @Size(max = 255) String posterPath,
                           @NotNull @Min(1) @Max(5) Integer calificacion, @Size(max = 500) String comentario,
                           boolean spoiler, @NotNull @Size(max = 100) Set<@NotNull @Positive Integer> temporadasVistas) {}
}
