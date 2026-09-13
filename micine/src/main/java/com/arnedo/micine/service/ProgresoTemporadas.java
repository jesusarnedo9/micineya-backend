package com.arnedo.micine.service;

import com.arnedo.micine.dto.TipoContenido;
import com.arnedo.micine.entity.Resena;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** La reseña pertenece a una temporada; el progreso incluye sus anteriores, sin crear reseñas. */
final class ProgresoTemporadas {
    private ProgresoTemporadas() {}

    static Set<Integer> vistas(Resena resena) {
        if (resena.getPelicula().getMediaType() != TipoContenido.SERIE) return Set.of();
        int ultima = resena.getNumeroTemporada() == null ? 0 : resena.getNumeroTemporada();
        ultima = Math.max(ultima, resena.getTemporadasVistas().stream()
                .filter(n -> n != null && n > 0).mapToInt(Integer::intValue).max().orElse(0));
        return hasta(ultima);
    }

    static Set<Integer> hasta(int numero) {
        return IntStream.rangeClosed(1, numero).boxed().collect(Collectors.toSet());
    }
}
