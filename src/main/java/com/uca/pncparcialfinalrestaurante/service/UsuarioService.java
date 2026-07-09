package com.uca.pncparcialfinalrestaurante.service;

import com.uca.pncparcialfinalrestaurante.domain.Role;
import com.uca.pncparcialfinalrestaurante.domain.Sucursal;
import com.uca.pncparcialfinalrestaurante.domain.Usuario;
import com.uca.pncparcialfinalrestaurante.exception.BusinessException;
import com.uca.pncparcialfinalrestaurante.exception.ResourceNotFoundException;
import com.uca.pncparcialfinalrestaurante.repository.UsuarioRepository;
import com.uca.pncparcialfinalrestaurante.web.dto.UsuarioDtos.CrearUsuarioRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.UsuarioDtos.UsuarioResponse;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión de usuarios por el Administrador. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SucursalService sucursalService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, SucursalService sucursalService,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.sucursalService = sucursalService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new BusinessException("El nombre de usuario ya está en uso");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setNombre(request.nombre());
        usuario.setRole(request.role());
        usuario.setActivo(true);

        // Un ENCARGADO DEBE pertenecer a una sucursal (es la base de la regla B).
        if (request.role() == Role.ENCARGADO) {
            if (request.sucursalId() == null) {
                throw new BusinessException("Un encargado debe tener una sucursal asignada");
            }
            Sucursal sucursal = sucursalService.obtenerEntidad(request.sucursalId());
            usuario.setSucursal(sucursal);
        } else if (request.sucursalId() != null) {
            // Para otros roles la sucursal es opcional; si viene, se respeta.
            usuario.setSucursal(sucursalService.obtenerEntidad(request.sucursalId()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarEstadoActivo(Long id, boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        usuario.setActivo(activo);
        return toResponse(usuarioRepository.save(usuario));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getUsername(),
                u.getNombre(),
                u.getRole(),
                u.getSucursal() != null ? u.getSucursal().getId() : null,
                u.isActivo());
    }
}
