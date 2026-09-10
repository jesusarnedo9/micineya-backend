package com.arnedo.micine.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "relaciones_usuario", uniqueConstraints = @UniqueConstraint(columnNames = {"origen_id", "destino_id", "tipo"}))
public class RelacionUsuario {
    public enum Tipo { SEGUIR, BLOQUEAR }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "origen_id")
    private Usuario origen;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "destino_id")
    private Usuario destino;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Tipo tipo;
    protected RelacionUsuario() {}
    public RelacionUsuario(Usuario origen, Usuario destino, Tipo tipo) {
        this.origen = origen; this.destino = destino; this.tipo = tipo;
    }
    public Usuario getOrigen() { return origen; }
    public Usuario getDestino() { return destino; }
    public Tipo getTipo() { return tipo; }
}
