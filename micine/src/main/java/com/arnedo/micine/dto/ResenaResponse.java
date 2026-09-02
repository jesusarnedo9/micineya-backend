package com.arnedo.micine.dto;

import java.time.LocalDateTime;

public record ResenaResponse(
        Long id,
        Long tmdbId,
        String titulo,
        String posterPath,
        Integer calificacion,
        String comentario,
        String autor,
        LocalDateTime fechaActualizacion
) {}
