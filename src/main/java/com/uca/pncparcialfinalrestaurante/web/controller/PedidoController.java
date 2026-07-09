package com.uca.pncparcialfinalrestaurante.web.controller;

import com.uca.pncparcialfinalrestaurante.security.CustomUserDetails;
import com.uca.pncparcialfinalrestaurante.service.PedidoService;
import com.uca.pncparcialfinalrestaurante.web.dto.PedidoDtos.CambiarEstadoPedidoRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.PedidoDtos.CrearPedidoRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.PedidoDtos.PedidoResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /** Crear pedido: cualquier usuario autenticado lo crea a su nombre. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@AuthenticationPrincipal CustomUserDetails actor,
                                @Valid @RequestBody CrearPedidoRequest request) {
        return pedidoService.crear(actor, request);
    }

    /** Listado filtrado por rol en la capa de negocio (admin/encargado/cliente). */
    @GetMapping
    public List<PedidoResponse> listar(@AuthenticationPrincipal CustomUserDetails actor) {
        return pedidoService.listar(actor);
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@AuthenticationPrincipal CustomUserDetails actor, @PathVariable Long id) {
        return pedidoService.obtener(actor, id);
    }

    /** Cambiar estado (confirmar, preparar, entregar): solo ADMIN o ENCARGADO (regla B). */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ENCARGADO')")
    public PedidoResponse cambiarEstado(@AuthenticationPrincipal CustomUserDetails actor,
                                        @PathVariable Long id,
                                        @Valid @RequestBody CambiarEstadoPedidoRequest request) {
        return pedidoService.cambiarEstado(actor, id, request.estado());
    }

    /** Cancelar: cliente (su pedido), encargado (su sucursal) o admin. */
    @PatchMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@AuthenticationPrincipal CustomUserDetails actor, @PathVariable Long id) {
        return pedidoService.cancelar(actor, id);
    }
}
