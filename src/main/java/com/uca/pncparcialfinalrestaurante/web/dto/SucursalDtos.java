package com.uca.pncparcialfinalrestaurante.web.dto;

import jakarta.validation.constraints.NotBlank;

/** DTOs de Sucursal agrupados. */
public final class SucursalDtos {

    private SucursalDtos() {
    }

    public record SucursalRequest(
            @NotBlank String nombre,
            @NotBlank String direccion) {
    }

    public record SucursalResponse(
            Long id,
            String nombre,
            String direccion) {
    }
}
