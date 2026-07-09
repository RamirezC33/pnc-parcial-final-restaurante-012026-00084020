package com.uca.pncparcialfinalrestaurante.web.dto;

import com.uca.pncparcialfinalrestaurante.domain.EstadoMesa;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class MesaDtos {

    private MesaDtos() {
    }

    public record MesaRequest(
            @NotNull Integer numero,
            @NotNull @Min(1) Integer capacidad,
            @NotNull Long sucursalId) {
    }

    public record CambiarEstadoMesaRequest(
            @NotNull EstadoMesa estado) {
    }

    public record MesaResponse(
            Long id,
            Integer numero,
            Integer capacidad,
            EstadoMesa estado,
            Long sucursalId) {
    }
}
