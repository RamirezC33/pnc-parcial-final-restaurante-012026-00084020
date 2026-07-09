package com.uca.pncparcialfinalrestaurante.service;

import com.uca.pncparcialfinalrestaurante.domain.Sucursal;
import com.uca.pncparcialfinalrestaurante.exception.ResourceNotFoundException;
import com.uca.pncparcialfinalrestaurante.repository.SucursalRepository;
import com.uca.pncparcialfinalrestaurante.web.dto.SucursalDtos.SucursalRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.SucursalDtos.SucursalResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SucursalService {

    private final SucursalRepository sucursalRepository;

    public SucursalService(SucursalRepository sucursalRepository) {
        this.sucursalRepository = sucursalRepository;
    }

    @Transactional(readOnly = true)
    public List<SucursalResponse> listar() {
        return sucursalRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Sucursal obtenerEntidad(Long id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public SucursalResponse obtener(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Transactional
    public SucursalResponse crear(SucursalRequest request) {
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre(request.nombre());
        sucursal.setDireccion(request.direccion());
        return toResponse(sucursalRepository.save(sucursal));
    }

    @Transactional
    public SucursalResponse actualizar(Long id, SucursalRequest request) {
        Sucursal sucursal = obtenerEntidad(id);
        sucursal.setNombre(request.nombre());
        sucursal.setDireccion(request.direccion());
        return toResponse(sucursalRepository.save(sucursal));
    }

    @Transactional
    public void eliminar(Long id) {
        Sucursal sucursal = obtenerEntidad(id);
        sucursalRepository.delete(sucursal);
    }

    private SucursalResponse toResponse(Sucursal s) {
        return new SucursalResponse(s.getId(), s.getNombre(), s.getDireccion());
    }
}
