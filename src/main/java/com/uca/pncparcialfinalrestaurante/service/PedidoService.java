package com.uca.pncparcialfinalrestaurante.service;

import com.uca.pncparcialfinalrestaurante.domain.DetallePedido;
import com.uca.pncparcialfinalrestaurante.domain.EstadoPedido;
import com.uca.pncparcialfinalrestaurante.domain.Mesa;
import com.uca.pncparcialfinalrestaurante.domain.Pedido;
import com.uca.pncparcialfinalrestaurante.domain.Producto;
import com.uca.pncparcialfinalrestaurante.domain.Role;
import com.uca.pncparcialfinalrestaurante.exception.BusinessException;
import com.uca.pncparcialfinalrestaurante.exception.ForbiddenOperationException;
import com.uca.pncparcialfinalrestaurante.exception.ResourceNotFoundException;
import com.uca.pncparcialfinalrestaurante.repository.PedidoRepository;
import com.uca.pncparcialfinalrestaurante.security.CustomUserDetails;
import com.uca.pncparcialfinalrestaurante.web.dto.PedidoDtos.CrearPedidoRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.PedidoDtos.DetalleResponse;
import com.uca.pncparcialfinalrestaurante.web.dto.PedidoDtos.ItemPedidoRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.PedidoDtos.PedidoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaService mesaService;
    private final ProductoService productoService;
    private final SucursalAccessGuard accessGuard;

    public PedidoService(PedidoRepository pedidoRepository, MesaService mesaService,
                         ProductoService productoService, SucursalAccessGuard accessGuard) {
        this.pedidoRepository = pedidoRepository;
        this.mesaService = mesaService;
        this.productoService = productoService;
        this.accessGuard = accessGuard;
    }

    /** Crea un pedido a nombre del usuario autenticado sobre una mesa concreta. */
    @Transactional
    public PedidoResponse crear(CustomUserDetails actor, CrearPedidoRequest request) {
        Mesa mesa = mesaService.obtenerEntidad(request.mesaId());

        Pedido pedido = new Pedido();
        pedido.setCliente(actor.getUsuario());
        pedido.setMesa(mesa);
        // Denormalizamos la sucursal desde la mesa: la regla B siempre compara
        // contra esta sucursal, aunque la mesa cambie después.
        pedido.setSucursal(mesa.getSucursal());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedidoRequest item : request.items()) {
            Producto producto = productoService.obtenerEntidad(item.productoId());
            if (!producto.isDisponible()) {
                throw new BusinessException("El producto no está disponible: " + producto.getNombre());
            }
            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(item.cantidad()));

            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(subtotal);
            pedido.agregarDetalle(detalle);

            total = total.add(subtotal);
        }
        pedido.setTotal(total);

        return toResponse(pedidoRepository.save(pedido));
    }

    /** Listado filtrado por rol: admin ve todo; encargado su sucursal; cliente lo suyo. */
    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(CustomUserDetails actor) {
        Role role = actor.getUsuario().getRole();
        List<Pedido> pedidos = switch (role) {
            case ADMINISTRADOR -> pedidoRepository.findAll();
            case ENCARGADO -> actor.getSucursalId() == null
                    ? List.of()
                    : pedidoRepository.findBySucursalId(actor.getSucursalId());
            case CLIENTE -> pedidoRepository.findByClienteId(actor.getId());
        };
        return pedidos.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(CustomUserDetails actor, Long id) {
        Pedido pedido = obtenerEntidad(id);
        verificarVisibilidad(actor, pedido);
        return toResponse(pedido);
    }

    /**
     * Cambia el estado de un pedido. Reservado a ADMIN y ENCARGADO. Para el
     * encargado se aplica la REGLA B: solo pedidos de su propia sucursal.
     */
    @Transactional
    public PedidoResponse cambiarEstado(CustomUserDetails actor, Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = obtenerEntidad(id);

        if (actor.getUsuario().getRole() == Role.ENCARGADO) {
            // Núcleo de la regla de negocio B.
            accessGuard.verificarGestionSucursal(actor, pedido.getSucursal().getId());
        }
        pedido.setEstado(nuevoEstado);
        return toResponse(pedidoRepository.save(pedido));
    }

    /** Cancela un pedido. Cliente solo el suyo; encargado los de su sucursal; admin cualquiera. */
    @Transactional
    public PedidoResponse cancelar(CustomUserDetails actor, Long id) {
        Pedido pedido = obtenerEntidad(id);
        Role role = actor.getUsuario().getRole();

        switch (role) {
            case CLIENTE -> {
                if (!pedido.getCliente().getId().equals(actor.getId())) {
                    throw new ForbiddenOperationException("Solo puede cancelar sus propios pedidos");
                }
            }
            // Regla B: el encargado solo cancela pedidos de su sucursal.
            case ENCARGADO -> accessGuard.verificarGestionSucursal(actor, pedido.getSucursal().getId());
            case ADMINISTRADOR -> { /* acceso total */ }
        }

        if (pedido.getEstado() == EstadoPedido.ENTREGADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new BusinessException("No se puede cancelar un pedido " + pedido.getEstado());
        }
        pedido.setEstado(EstadoPedido.CANCELADO);
        return toResponse(pedidoRepository.save(pedido));
    }

    private Pedido obtenerEntidad(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
    }

    /** Reglas de visibilidad de lectura, coherentes con el filtrado del listado. */
    private void verificarVisibilidad(CustomUserDetails actor, Pedido pedido) {
        switch (actor.getUsuario().getRole()) {
            case ADMINISTRADOR -> { /* ve todo */ }
            case ENCARGADO -> accessGuard.verificarGestionSucursal(actor, pedido.getSucursal().getId());
            case CLIENTE -> {
                if (!pedido.getCliente().getId().equals(actor.getId())) {
                    throw new ForbiddenOperationException("No puede ver pedidos de otros clientes");
                }
            }
        }
    }

    private PedidoResponse toResponse(Pedido p) {
        List<DetalleResponse> detalles = p.getDetalles().stream()
                .map(d -> new DetalleResponse(
                        d.getProducto().getId(),
                        d.getProducto().getNombre(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getSubtotal()))
                .toList();
        return new PedidoResponse(
                p.getId(),
                p.getCliente().getUsername(),
                p.getMesa().getId(),
                p.getSucursal().getId(),
                p.getEstado(),
                p.getTotal(),
                p.getFechaCreacion(),
                detalles);
    }
}
