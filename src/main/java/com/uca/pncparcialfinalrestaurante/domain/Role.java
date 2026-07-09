package com.uca.pncparcialfinalrestaurante.domain;

/**
 * Roles del sistema. Se persisten como texto (EnumType.STRING) y se exponen a
 * Spring Security con el prefijo "ROLE_" (ver {@link Usuario#getAuthorities()}).
 */
public enum Role {
    /** Acceso total a todas las sucursales. */
    ADMINISTRADOR,
    /** Gestiona pedidos y mesas ÚNICAMENTE de su propia sucursal. */
    ENCARGADO,
    /** Solo crea, ve y cancela sus propios pedidos. */
    CLIENTE
}
