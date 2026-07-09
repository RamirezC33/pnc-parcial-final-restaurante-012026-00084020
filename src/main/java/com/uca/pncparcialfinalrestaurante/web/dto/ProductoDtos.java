package com.uca.pncparcialfinalrestaurante.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class ProductoDtos {

    private ProductoDtos() {
    }

    public record ProductoRequest(
            @NotBlank String nombre,
            String descripcion,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal precio,
            boolean disponible) {
    }

    public record ProductoResponse(
            Long id,
            String nombre,
            String descripcion,
            BigDecimal precio,
            boolean disponible) {
    }
}
