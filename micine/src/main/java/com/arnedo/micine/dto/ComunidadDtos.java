package com.arnedo.micine.dto;

import com.arnedo.micine.entity.ReporteComunidad.Motivo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;

public final class ComunidadDtos {
    private ComunidadDtos() {}
    public record Estado(Long miId, boolean normasAceptadas, String versionNormas, boolean puedeModerar, boolean suspendida) {}
    public record AceptarNormas(@NotNull String version) {}
    // Nunca serializar Usuario: estos DTO no contienen email, preferencias, guardadas ni red de seguidores.
    public record Persona(Long id, String username, String foto, boolean siguiendo) {}
    public record Publicacion(Long id, Long autorId, String username, Long tmdbId, String titulo,
                              String posterPath, int calificacion, String comentario, boolean spoiler, LocalDateTime fecha,
                              TipoContenido mediaType) {}
    public record Perfil(Persona persona, List<Publicacion> publicaciones, boolean hayMas, ProgresoResponse progreso) {}
    public record Pagina(List<Publicacion> publicaciones, boolean hayMas) {}
    public record Reportar(@NotNull @Positive Long usuarioId, @Positive Long resenaId, @NotNull Motivo motivo) {}
    public enum Accion { OCULTAR_RESENA, MARCAR_SPOILER, QUITAR_FOTO, SUSPENDER, DESESTIMAR }
    public record Resolver(@NotNull Accion accion) {}
    public record Reporte(Long id, Long usuarioId, String username, String foto, Publicacion publicacion,
                          Motivo motivo, LocalDateTime creado) {}
}
