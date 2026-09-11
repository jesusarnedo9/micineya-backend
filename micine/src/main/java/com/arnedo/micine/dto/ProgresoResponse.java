package com.arnedo.micine.dto;

public record ProgresoResponse(Long usuarioId, long peliculasVistas, int capacidadBalde,
                               long baldesCompletos, int pochoclosEnBalde, long numeroBalde,
                               long seriesVistas, long temporadasVistas, long totalPochoclos,
                               java.util.List<String> insignias) {}
