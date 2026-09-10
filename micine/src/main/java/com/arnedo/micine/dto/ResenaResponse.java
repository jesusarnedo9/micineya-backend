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
        LocalDateTime fechaActualizacion,
        boolean spoiler,
        boolean ocultadaModeracion,
        TipoContenido mediaType,
        java.util.Set<Integer> temporadasVistas,
        LocalDateTime fechaVista
) {}
