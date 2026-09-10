package com.arnedo.micine.dto;

public record DisponibilidadTemporadaResponse(Long tmdbId, TipoContenido mediaType, int numero,
        int episodiosCatalogados, int episodiosEstrenados, boolean disponibleParaMarcar,
        String motivo) {}
