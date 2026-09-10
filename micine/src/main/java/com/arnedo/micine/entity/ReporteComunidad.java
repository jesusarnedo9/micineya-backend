package com.arnedo.micine.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes_comunidad", indexes = @Index(columnList = "estado,creado"))
public class ReporteComunidad {
    public enum Motivo { ACOSO, CONTENIDO_INAPROPIADO, DATOS_PERSONALES, SPAM, SPOILER, OTRO }
    public enum Estado { PENDIENTE, RESUELTO, DESESTIMADO }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // IDs escalares: al borrar la cuenta/reseña se purgan los reportes, sin conservar copias del contenido.
    @Column(nullable = false) private Long denuncianteId;
    @Column(nullable = false) private Long usuarioId;
    private Long resenaId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Motivo motivo;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Estado estado = Estado.PENDIENTE;
    @Column(nullable = false) private LocalDateTime creado = LocalDateTime.now();
    private Long moderadorId;
    private String accion;
    private LocalDateTime resuelto;
    protected ReporteComunidad() {}
    public ReporteComunidad(Long denunciante, Long usuario, Long resena, Motivo motivo) {
        this.denuncianteId = denunciante; this.usuarioId = usuario; this.resenaId = resena; this.motivo = motivo;
    }
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getResenaId() { return resenaId; }
    public Motivo getMotivo() { return motivo; }
    public Estado getEstado() { return estado; }
    public LocalDateTime getCreado() { return creado; }
    public void resolver(Long moderador, String accion) {
        this.moderadorId = moderador; this.accion = accion; this.resuelto = LocalDateTime.now();
        this.estado = "DESESTIMAR".equals(accion) ? Estado.DESESTIMADO : Estado.RESUELTO;
    }
}
