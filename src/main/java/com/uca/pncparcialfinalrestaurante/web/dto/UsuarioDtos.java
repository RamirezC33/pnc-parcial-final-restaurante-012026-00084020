package com.uca.pncparcialfinalrestaurante.web.dto;

import com.uca.pncparcialfinalrestaurante.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    /** Alta de usuarios por el Administrador (puede asignar rol y sucursal). */
    public record CrearUsuarioRequest(
            @NotBlank String username,
            @NotBlank @Size(min = 6) String password,
            @NotBlank String nombre,
            @NotNull Role role,
            /** Requerido para ENCARGADO; ignorable para ADMINISTRADOR/CLIENTE. */
            Long sucursalId) {
    }

    public record UsuarioResponse(
            Long id,
            String username,
            String nombre,
            Role role,
            Long sucursalId,
            boolean activo) {
    }
}
