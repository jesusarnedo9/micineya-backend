package com.arnedo.micine.service;

import com.arnedo.micine.dto.ComunidadDtos.*;
import com.arnedo.micine.entity.*;
import com.arnedo.micine.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
import static com.arnedo.micine.entity.RelacionUsuario.Tipo.*;

@Service
@Transactional(readOnly = true)
public class ComunidadService {
    public static final String VERSION_NORMAS = "2026-09-10";
    private static final int TAMANO = 20;
    private final UsuarioRepository usuarios;
    private final RelacionUsuarioRepository relaciones;
    private final ResenaRepository resenas;
    private final FotoPerfilRepository fotos;
    private final ReporteComunidadRepository reportes;
    private final Set<Long> moderadores;
    private final ProgresoService progreso;

    public ComunidadService(UsuarioRepository usuarios, RelacionUsuarioRepository relaciones,
                            ResenaRepository resenas, FotoPerfilRepository fotos, ReporteComunidadRepository reportes,
                            @Value("${comunidad.moderator-user-ids:}") String moderatorIds, ProgresoService progreso) {
        this.usuarios = usuarios; this.relaciones = relaciones; this.resenas = resenas;
        this.fotos = fotos; this.reportes = reportes;
        this.moderadores = new HashSet<>();
        this.progreso = progreso;
        for (String id : moderatorIds.split(",")) if (!id.isBlank()) moderadores.add(Long.parseLong(id.trim()));
    }

    public Estado estado(String email) {
        Usuario yo = usuario(email);
        return new Estado(yo.getId(), participa(yo), VERSION_NORMAS, moderadores.contains(yo.getId()), yo.isComunidadSuspendida());
    }

    @Transactional
    public void aceptar(String email, String version) {
        if (!VERSION_NORMAS.equals(version)) throw new IllegalArgumentException("Revisá la versión actual de las normas");
        Usuario yo = usuarios.findByEmailForUpdate(email).orElseThrow(ComunidadService::noDisponible);
        activo(yo);
        yo.setNormasComunidadVersion(VERSION_NORMAS);
    }

    public List<Persona> buscar(String email, String query) {
        Usuario yo = usuario(email);
        String texto = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (texto.length() < 2) return List.of();
        if (texto.length() > 50) throw new IllegalArgumentException("Buscá un nombre de hasta 50 caracteres");
        Set<Long> excluidos = bloqueados(yo.getId());
        excluidos.add(yo.getId());
        // Escapar comodines evita convertir una búsqueda por nombre en un listado completo.
        String patron = "%" + texto.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Set<Long> siguiendo = siguiendo(yo.getId());
        return usuarios.buscarPublicos(patron, VERSION_NORMAS, excluidos, PageRequest.of(0, TAMANO)).stream()
                .map(u -> persona(u, siguiendo.contains(u.getId()))).toList();
    }

    public Perfil perfil(String email, Long id, int pagina) {
        Usuario yo = usuario(email);
        Usuario otro = visible(yo, id);
        Pagina publicaciones = pagina(Set.of(id), pagina);
        return new Perfil(persona(otro, siguiendo(yo.getId()).contains(id)), publicaciones.publicaciones(), publicaciones.hayMas(), progreso.paraUsuario(id));
    }

    public Pagina feed(String email, int pagina) {
        Usuario yo = usuario(email);
        Set<Long> ids = siguiendo(yo.getId());
        ids.removeAll(bloqueados(yo.getId()));
        return pagina(ids, pagina);
    }

    private Pagina pagina(Set<Long> ids, int pagina) {
        if (pagina < 0 || pagina > 1000) throw new IllegalArgumentException("Página inválida");
        if (ids.isEmpty()) return new Pagina(List.of(), false);
        // Una ventana de 20; consultar la siguiente fila no requiere cargar el historial completo.
        var items = resenas.publicaciones(ids, VERSION_NORMAS, PageRequest.of(pagina, TAMANO));
        boolean hayMas = items.size() == TAMANO
                && !resenas.publicaciones(ids, VERSION_NORMAS, PageRequest.of((pagina + 1) * TAMANO, 1)).isEmpty();
        return new Pagina(items.stream().map(this::publicacion).toList(), hayMas);
    }

    @Transactional
    public void seguir(String email, Long id, boolean seguir) {
        Usuario yo = usuario(email);
        Usuario otro = bloquearPar(yo, id);
        if (!seguir) {
            relaciones.deleteByOrigenIdAndDestinoIdAndTipo(yo.getId(), id, SEGUIR);
            return;
        }
        activo(yo);
        if (!participa(yo)) throw new IllegalArgumentException("Aceptá las normas de Comunidad para seguir a alguien");
        visible(yo, id);
        if (!relaciones.existsByOrigenIdAndDestinoIdAndTipo(yo.getId(), id, SEGUIR)) {
            relaciones.save(new RelacionUsuario(yo, otro, SEGUIR));
        }
    }

    @Transactional
    public void bloquear(String email, Long id, boolean bloquear) {
        Usuario yo = usuario(email);
        Usuario otro = bloquearPar(yo, id);
        if (!bloquear) {
            relaciones.deleteByOrigenIdAndDestinoIdAndTipo(yo.getId(), id, BLOQUEAR);
            return;
        }
        relaciones.deleteByOrigenIdAndDestinoIdAndTipo(yo.getId(), id, SEGUIR);
        relaciones.deleteByOrigenIdAndDestinoIdAndTipo(id, yo.getId(), SEGUIR);
        if (!relaciones.existsByOrigenIdAndDestinoIdAndTipo(yo.getId(), id, BLOQUEAR)) {
            relaciones.save(new RelacionUsuario(yo, otro, BLOQUEAR));
        }
    }

    public List<Persona> misBloqueados(String email) {
        return relaciones.findByOrigenIdAndTipo(usuario(email).getId(), BLOQUEAR).stream()
                .map(r -> new Persona(r.getDestino().getId(), r.getDestino().getUsername(), null, false)).toList();
    }

    public boolean puedeVerResena(Usuario yo, Resena resena) {
        if (yo.getId().equals(resena.getUsuario().getId())) return true;
        return publica(resena) && !bloqueados(yo.getId()).contains(resena.getUsuario().getId());
    }

    @Transactional
    public void reportar(String email, Reportar request) {
        Usuario yo = usuario(email);
        bloquearPar(yo, request.usuarioId());
        visible(yo, request.usuarioId());
        if (request.resenaId() != null) {
            Resena resena = resenas.findById(request.resenaId()).orElseThrow(ComunidadService::noDisponible);
            if (!resena.getUsuario().getId().equals(request.usuarioId()) || !publica(resena)) throw noDisponible();
        }
        if (reportes.existsByDenuncianteIdAndUsuarioIdAndResenaIdAndEstado(yo.getId(), request.usuarioId(), request.resenaId(), ReporteComunidad.Estado.PENDIENTE)) return;
        if (reportes.countByDenuncianteIdAndCreadoAfter(yo.getId(), LocalDateTime.now().minusDays(1)) >= 10) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Ya enviaste diez reportes hoy. Podés bloquear la cuenta mientras se revisan");
        }
        reportes.save(new ReporteComunidad(yo.getId(), request.usuarioId(), request.resenaId(), request.motivo()));
    }

    public List<Reporte> pendientes(String email) {
        moderador(email);
        return reportes.findTop50ByEstadoOrderByCreadoAsc(ReporteComunidad.Estado.PENDIENTE).stream().map(r -> {
            Usuario autor = usuarios.findById(r.getUsuarioId()).orElseThrow(ComunidadService::noDisponible);
            var post = r.getResenaId() == null ? null : resenas.findById(r.getResenaId()).map(this::publicacion).orElse(null);
            return new Reporte(r.getId(), autor.getId(), autor.getUsername(), foto(autor.getId()), post, r.getMotivo(), r.getCreado());
        }).toList();
    }

    @Transactional
    public void resolver(String email, Long id, Accion accion) {
        Usuario admin = moderador(email);
        var existente = reportes.findById(id).orElseThrow(ComunidadService::noDisponible);
        Usuario autor = usuarios.findByIdForUpdate(existente.getUsuarioId()).orElseThrow(ComunidadService::noDisponible);
        var reporte = reportes.bloquear(id).orElseThrow(ComunidadService::noDisponible);
        if (reporte.getEstado() != ReporteComunidad.Estado.PENDIENTE) return;
        switch (accion) {
            case OCULTAR_RESENA, MARCAR_SPOILER -> {
                if (reporte.getResenaId() == null) throw new IllegalArgumentException("Este reporte no corresponde a una reseña");
                var resena = resenas.findById(reporte.getResenaId()).orElseThrow(ComunidadService::noDisponible);
                if (accion == Accion.OCULTAR_RESENA) resena.setOcultadaModeracion(true);
                else resena.setSpoiler(true);
            }
            case QUITAR_FOTO -> fotos.deleteById(autor.getId());
            case SUSPENDER -> {
                if (moderadores.contains(autor.getId())) throw new IllegalArgumentException("Revisá este caso fuera del panel de moderación");
                autor.setComunidadSuspendida(true);
                relaciones.eliminarCuenta(autor.getId());
            }
            case DESESTIMAR -> { }
        }
        reporte.resolver(admin.getId(), accion.name());
    }

    private Usuario moderador(String email) {
        Usuario yo = usuario(email);
        if (!moderadores.contains(yo.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tenés permisos de moderación");
        return yo;
    }
    private Usuario bloquearPar(Usuario yo, Long id) {
        if (id == null || id.equals(yo.getId())) throw new IllegalArgumentException("Elegí otra persona");
        // Siempre el mismo orden: serializa seguir/bloquear y evita relaciones recreadas durante un bloqueo.
        usuarios.findByIdForUpdate(Math.min(yo.getId(), id)).orElseThrow(ComunidadService::noDisponible);
        usuarios.findByIdForUpdate(Math.max(yo.getId(), id)).orElseThrow(ComunidadService::noDisponible);
        return usuarios.findById(id).orElseThrow(ComunidadService::noDisponible);
    }
    private Usuario visible(Usuario yo, Long id) {
        Usuario otro = usuarios.findById(id).orElseThrow(ComunidadService::noDisponible);
        if (!participa(otro) || otro.isComunidadSuspendida() || bloqueados(yo.getId()).contains(id)) throw noDisponible();
        return otro;
    }
    private Usuario usuario(String email) { return usuarios.findByEmail(email).orElseThrow(ComunidadService::noDisponible); }
    private boolean participa(Usuario u) { return VERSION_NORMAS.equals(u.getNormasComunidadVersion()); }
    private boolean publica(Resena r) { return participa(r.getUsuario()) && !r.getUsuario().isComunidadSuspendida() && !r.isOcultadaModeracion(); }
    private void activo(Usuario u) {
        if (u.isComunidadSuspendida()) throw new IllegalArgumentException("Tu participación en Comunidad está suspendida. Podés seguir usando tu biblioteca privada");
    }
    private Set<Long> siguiendo(Long id) {
        Set<Long> ids = new HashSet<>();
        relaciones.findByOrigenIdAndTipo(id, SEGUIR).forEach(r -> ids.add(r.getDestino().getId()));
        return ids;
    }
    private Set<Long> bloqueados(Long id) {
        Set<Long> ids = new HashSet<>();
        relaciones.relacionadas(id, BLOQUEAR).forEach(r -> ids.add(r.getOrigen().getId().equals(id) ? r.getDestino().getId() : r.getOrigen().getId()));
        return ids;
    }
    private Persona persona(Usuario u, boolean siguiendo) { return new Persona(u.getId(), u.getUsername(), foto(u.getId()), siguiendo); }
    private String foto(Long id) { return fotos.findById(id).map(f -> "data:image/jpeg;base64," + f.getBase64()).orElse(null); }
    private Publicacion publicacion(Resena r) {
        return new Publicacion(r.getId(), r.getUsuario().getId(), r.getUsuario().getUsername(), r.getPelicula().getTmdbId(),
                r.getPelicula().getTitulo(), r.getPelicula().getPosterPath(), r.getCalificacion(), r.getComentario(), r.isSpoiler(), r.getFechaActualizacion(), r.getPelicula().getMediaType());
    }
    private static ResponseStatusException noDisponible() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Este perfil o contenido no está disponible"); }
}
