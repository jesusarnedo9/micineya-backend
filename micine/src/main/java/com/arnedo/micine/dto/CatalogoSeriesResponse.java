package com.arnedo.micine.dto;

import java.util.List;

/** Catálogo de solo lectura; no es todavía el lote persistido de recomendaciones. */
public record CatalogoSeriesResponse(List<PeliculaDto> results, List<String> generosSinEquivalencia) {}
