package com.arnedo.micine.dto;

import java.util.Set;

public record PerfilRecomendacion(
        Set<Integer> generoIds,
        Set<Integer> plataformaIds,
        Set<Long> peliculasVistasIds,
        Set<Long> peliculasFavoritasIds,
        Set<Long> peliculasDescartadasIds,
        Set<Long> recomendacionesRecientesIds
) {
    public PerfilRecomendacion(Set<Integer> generos, Set<Integer> plataformas,
                               Set<Long> vistas, Set<Long> favoritas) {
        this(generos, plataformas, vistas, favoritas, Set.of(), Set.of());
    }
}
