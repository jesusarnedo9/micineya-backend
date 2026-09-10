package com.arnedo.micine.dto;

public record ProgresoResponse(Long usuarioId, long peliculasVistas, int capacidadBalde,
                               long baldesCompletos, int pochoclosEnBalde, long numeroBalde) {}
