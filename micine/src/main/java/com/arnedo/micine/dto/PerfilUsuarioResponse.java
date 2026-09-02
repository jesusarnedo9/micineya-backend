package com.arnedo.micine.dto;

public record PerfilUsuarioResponse(
        String username,
        String email,
        long peliculasVistas,
        long peliculasGuardadas,
        long resenasConComentario
) {}
