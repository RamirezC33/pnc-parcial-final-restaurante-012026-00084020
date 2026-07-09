package com.uca.pncparcialfinalrestaurante.service;

import com.uca.pncparcialfinalrestaurante.domain.RefreshToken;
import com.uca.pncparcialfinalrestaurante.domain.Usuario;
import com.uca.pncparcialfinalrestaurante.exception.BusinessException;
import com.uca.pncparcialfinalrestaurante.repository.RefreshTokenRepository;
import com.uca.pncparcialfinalrestaurante.security.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestiona los Refresh Tokens con estado en base de datos. Al persistirlos
 * podemos revocarlos antes de que expiren (logout, rotación, cambio de contraseña).
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationDays = properties.getRefreshExpirationDays();
    }

    @Transactional
    public RefreshToken crear(Usuario usuario) {
        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Instant.now().plus(Duration.ofDays(refreshExpirationDays)));
        token.setRevoked(false);
        return refreshTokenRepository.save(token);
    }

    /** Valida el refresh token; lanza excepción si no existe, expiró o fue revocado. */
    @Transactional(readOnly = true)
    public RefreshToken validar(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException("Refresh token inválido"));
        if (!token.isActive()) {
            throw new BusinessException("Refresh token expirado o revocado. Inicie sesión de nuevo.");
        }
        return token;
    }

    /**
     * Rotación: revoca el refresh token usado y emite uno nuevo. Evita que un
     * refresh token robado siga siendo válido tras usarse.
     */
    @Transactional
    public RefreshToken rotar(RefreshToken actual) {
        actual.setRevoked(true);
        refreshTokenRepository.save(actual);
        return crear(actual.getUsuario());
    }

    /** Revoca TODOS los refresh tokens de un usuario (logout global / cambio de contraseña). */
    @Transactional
    public void revocarTodos(Usuario usuario) {
        refreshTokenRepository.revokeAllForUsuario(usuario);
    }
}
