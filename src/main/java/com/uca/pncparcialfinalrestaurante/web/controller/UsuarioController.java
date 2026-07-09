package com.uca.pncparcialfinalrestaurante.web.controller;

import com.uca.pncparcialfinalrestaurante.service.UsuarioService;
import com.uca.pncparcialfinalrestaurante.web.dto.UsuarioDtos.CrearUsuarioRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.UsuarioDtos.UsuarioResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de usuarios: exclusiva del Administrador. */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PatchMapping("/{id}/activo")
    public UsuarioResponse cambiarEstadoActivo(@PathVariable Long id, @RequestParam boolean activo) {
        return usuarioService.cambiarEstadoActivo(id, activo);
    }
}
