package com.uca.pncparcialfinalrestaurante.web.controller;

import com.uca.pncparcialfinalrestaurante.security.CustomUserDetails;
import com.uca.pncparcialfinalrestaurante.service.MesaService;
import com.uca.pncparcialfinalrestaurante.web.dto.MesaDtos.CambiarEstadoMesaRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.MesaDtos.MesaRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.MesaDtos.MesaResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    /** El servicio filtra: el encargado solo ve las mesas de su sucursal. */
    @GetMapping
    public List<MesaResponse> listar(@AuthenticationPrincipal CustomUserDetails actor) {
        return mesaService.listar(actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ENCARGADO')")
    public MesaResponse crear(@AuthenticationPrincipal CustomUserDetails actor,
                              @Valid @RequestBody MesaRequest request) {
        return mesaService.crear(actor, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ENCARGADO')")
    public MesaResponse cambiarEstado(@AuthenticationPrincipal CustomUserDetails actor,
                                      @PathVariable Long id,
                                      @Valid @RequestBody CambiarEstadoMesaRequest request) {
        return mesaService.cambiarEstado(actor, id, request.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ENCARGADO')")
    public void eliminar(@AuthenticationPrincipal CustomUserDetails actor, @PathVariable Long id) {
        mesaService.eliminar(actor, id);
    }
}
