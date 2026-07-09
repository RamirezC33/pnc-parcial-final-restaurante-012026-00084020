package com.uca.pncparcialfinalrestaurante.service;

import com.uca.pncparcialfinalrestaurante.domain.Producto;
import com.uca.pncparcialfinalrestaurante.exception.ResourceNotFoundException;
import com.uca.pncparcialfinalrestaurante.repository.ProductoRepository;
import com.uca.pncparcialfinalrestaurante.web.dto.ProductoDtos.ProductoRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.ProductoDtos.ProductoResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Producto obtenerEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto p = new Producto();
        aplicar(p, request);
        return toResponse(productoRepository.save(p));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto p = obtenerEntidad(id);
        aplicar(p, request);
        return toResponse(productoRepository.save(p));
    }

    @Transactional
    public void eliminar(Long id) {
        productoRepository.delete(obtenerEntidad(id));
    }

    private void aplicar(Producto p, ProductoRequest request) {
        p.setNombre(request.nombre());
        p.setDescripcion(request.descripcion());
        p.setPrecio(request.precio());
        p.setDisponible(request.disponible());
    }

    private ProductoResponse toResponse(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.isDisponible());
    }
}
