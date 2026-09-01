package com.arnedo.micine.dto;

import java.util.Set;

public record PerfilRecomendacion(
        Set<Integer> generoIds,
        Set<Integer> plataformaIds,
        Set<Long> peliculasVistasIds,
        Set<Long> peliculasFavoritasIds
) {
}
