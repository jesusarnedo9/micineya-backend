package com.arnedo.micine.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario_foto_perfil")
public class FotoPerfil {
    @Id
    private Long usuarioId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, columnDefinition = "text")
    private String base64;

    protected FotoPerfil() {}

    public FotoPerfil(Usuario usuario, String base64) {
        this.usuario = usuario;
        this.base64 = base64;
    }

    public String getBase64() { return base64; }
    public void setBase64(String base64) { this.base64 = base64; }
}
