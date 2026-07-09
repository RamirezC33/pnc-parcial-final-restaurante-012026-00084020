package com.uca.pncparcialfinalrestaurante.web.dto;

import com.uca.pncparcialfinalrestaurante.domain.EstadoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PedidoDtos {

    private PedidoDtos() {
    }

    public record ItemPedidoRequest(
            @NotNull Long productoId,
            @NotNull @Min(1) Integer cantidad) {
    }

    public record CrearPedidoRequest(
            @NotNull Long mesaId,
            @NotEmpty @Valid List<ItemPedidoRequest> items) {
    }

    public record CambiarEstadoPedidoRequest(
            @NotNull EstadoPedido estado) {
    }

    public record DetalleResponse(
            Long productoId,
            String productoNombre,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {
    }

    public record PedidoResponse(
            Long id,
            String clienteUsername,
            Long mesaId,
            Long sucursalId,
            EstadoPedido estado,
            BigDecimal total,
            Instant fechaCreacion,
            List<DetalleResponse> detalles) {
    }
}
