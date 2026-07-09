package com.uca.pncparcialfinalrestaurante.service;

import com.uca.pncparcialfinalrestaurante.domain.Mesa;
import com.uca.pncparcialfinalrestaurante.domain.Role;
import com.uca.pncparcialfinalrestaurante.domain.Sucursal;
import com.uca.pncparcialfinalrestaurante.exception.ResourceNotFoundException;
import com.uca.pncparcialfinalrestaurante.repository.MesaRepository;
import com.uca.pncparcialfinalrestaurante.security.CustomUserDetails;
import com.uca.pncparcialfinalrestaurante.web.dto.MesaDtos.MesaRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.MesaDtos.MesaResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MesaService {

    private final MesaRepository mesaRepository;
    private final SucursalService sucursalService;
    private final SucursalAccessGuard accessGuard;

    public MesaService(MesaRepository mesaRepository, SucursalService sucursalService,
                       SucursalAccessGuard accessGuard) {
        this.mesaRepository = mesaRepository;
        this.sucursalService = sucursalService;
        this.accessGuard = accessGuard;
    }

    /** Admin ve todas las mesas; el encargado solo las de su sucursal. */
    @Transactional(readOnly = true)
    public List<MesaResponse> listar(CustomUserDetails actor) {
        List<Mesa> mesas;
        if (actor.getUsuario().getRole() == Role.ENCARGADO && actor.getSucursalId() != null) {
            mesas = mesaRepository.findBySucursalId(actor.getSucursalId());
        } else {
            mesas = mesaRepository.findAll();
        }
        return mesas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Mesa obtenerEntidad(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + id));
    }

    @Transactional
    public MesaResponse crear(CustomUserDetails actor, MesaRequest request) {
        // Regla B: un encargado solo puede crear mesas en su propia sucursal.
        accessGuard.verificarGestionSucursal(actor, request.sucursalId());

        Sucursal sucursal = sucursalService.obtenerEntidad(request.sucursalId());
        Mesa mesa = new Mesa();
        mesa.setNumero(request.numero());
        mesa.setCapacidad(request.capacidad());
        mesa.setSucursal(sucursal);
        return toResponse(mesaRepository.save(mesa));
    }

    @Transactional
    public MesaResponse cambiarEstado(CustomUserDetails actor, Long id,
                                      com.uca.pncparcialfinalrestaurante.domain.EstadoMesa estado) {
        Mesa mesa = obtenerEntidad(id);
        // Regla B: valida contra la sucursal REAL de la mesa.
        accessGuard.verificarGestionSucursal(actor, mesa.getSucursal().getId());
        mesa.setEstado(estado);
        return toResponse(mesaRepository.save(mesa));
    }

    @Transactional
    public void eliminar(CustomUserDetails actor, Long id) {
        Mesa mesa = obtenerEntidad(id);
        accessGuard.verificarGestionSucursal(actor, mesa.getSucursal().getId());
        mesaRepository.delete(mesa);
    }

    private MesaResponse toResponse(Mesa m) {
        return new MesaResponse(m.getId(), m.getNumero(), m.getCapacidad(), m.getEstado(),
                m.getSucursal().getId());
    }
}
