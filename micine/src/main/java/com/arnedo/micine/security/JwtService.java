package com.arnedo.micine.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import com.arnedo.micine.entity.Usuario;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarAccessToken(String email, int tokenVersion, Long usuarioId) {
        return generarToken(email, tokenVersion, usuarioId, ACCESS_TOKEN_TYPE, expirationMs);
    }

    public String generarRefreshToken(String email, int tokenVersion, Long usuarioId) {
        return generarToken(email, tokenVersion, usuarioId, REFRESH_TOKEN_TYPE, refreshExpirationMs);
    }

    private String generarToken(String email, int tokenVersion, Long usuarioId, String tipo, long duracionMs) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + duracionMs);

        return Jwts.builder()
                .subject(email)
                .claim("ver", tokenVersion)
                .claim("uid", usuarioId)
                .claim(TOKEN_TYPE_CLAIM, tipo)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(getKey())
                .compact();
    }

    public String extraerEmail(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean correspondeAUsuario(String token, Usuario usuario) {
        try {
            Long id = Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token)
                    .getPayload().get("uid", Long.class);
            return id == null ? !usuario.requiereTokenConId() : id.equals(usuario.getId());
        } catch (Exception ex) {
            return false;
        }
    }

    public int extraerTokenVersion(String token) {
        Integer version = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("ver", Integer.class);
        return version == null ? 0 : version;
    }

    public boolean esTokenValido(String token, String email, int tokenVersion) {
        try {
            return extraerEmail(token).equals(email)
                    && extraerTokenVersion(token) == tokenVersion
                    && esAccessToken(token)
                    && !estaExpirado(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean esRefreshTokenValido(String token, String email, int tokenVersion) {
        try {
            return extraerEmail(token).equals(email)
                    && extraerTokenVersion(token) == tokenVersion
                    && REFRESH_TOKEN_TYPE.equals(extraerTipo(token))
                    && !estaExpirado(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean esAccessToken(String token) {
        String tipo = extraerTipo(token);
        // Los tokens emitidos antes de esta versión no tenían tipo y siguen siendo
        // válidos hasta su vencimiento natural.
        return tipo == null || ACCESS_TOKEN_TYPE.equals(tipo);
    }

    private String extraerTipo(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get(TOKEN_TYPE_CLAIM, String.class);
    }

    private boolean estaExpirado(String token) {
        Date expiracion = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expiracion.before(new Date());
    }
}
