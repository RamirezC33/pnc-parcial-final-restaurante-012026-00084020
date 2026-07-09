package com.uca.pncparcialfinalrestaurante.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Genera y valida el Access Token (JWT firmado con HS256).
 *
 * <p>El Access Token es <b>stateless</b>: lleva dentro el usuario, su rol y su
 * sucursal, y se valida solo con la firma. El Refresh Token, en cambio, se
 * gestiona con estado en BD (ver {@code RefreshTokenService}) para poder revocarlo.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpirationMinutes;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMinutes = properties.getAccessExpirationMinutes();
    }

    public String generateAccessToken(CustomUserDetails user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(accessExpirationMinutes));

        var builder = Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getUsuario().getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        // La sucursal viaja en el token para poder aplicar la regla B sin ir a BD.
        if (user.getSucursalId() != null) {
            builder.claim("sucursalId", user.getSucursalId());
        }
        return builder.signWith(key).compact();
    }

    /** Devuelve el username (subject) si el token es válido; lanza excepción si no. */
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
