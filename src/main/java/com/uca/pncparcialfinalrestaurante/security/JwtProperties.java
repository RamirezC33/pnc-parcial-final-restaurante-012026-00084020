package com.uca.pncparcialfinalrestaurante.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración de JWT (prefijo {@code app.jwt} en application.yaml).
 * El secreto NUNCA se hardcodea: se inyecta por variable de entorno.
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Secreto HMAC para firmar los tokens (mínimo 32 bytes para HS256). */
    private String secret;

    /** Minutos de validez del Access Token (corto, p.ej. 15). */
    private long accessExpirationMinutes = 15;

    /** Días de validez del Refresh Token (mayor, p.ej. 7). */
    private long refreshExpirationDays = 7;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessExpirationMinutes() {
        return accessExpirationMinutes;
    }

    public void setAccessExpirationMinutes(long accessExpirationMinutes) {
        this.accessExpirationMinutes = accessExpirationMinutes;
    }

    public long getRefreshExpirationDays() {
        return refreshExpirationDays;
    }

    public void setRefreshExpirationDays(long refreshExpirationDays) {
        this.refreshExpirationDays = refreshExpirationDays;
    }
}
