package com.uca.pncparcialfinalrestaurante.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uca.pncparcialfinalrestaurante.domain.Role;
import com.uca.pncparcialfinalrestaurante.domain.Sucursal;
import com.uca.pncparcialfinalrestaurante.domain.Usuario;
import com.uca.pncparcialfinalrestaurante.exception.ForbiddenOperationException;
import com.uca.pncparcialfinalrestaurante.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la REGLA DE NEGOCIO B (autorización por sucursal). No necesita
 * contexto de Spring: valida directamente la lógica de comparación de sucursal.
 */
class SucursalAccessGuardTest {

    private final SucursalAccessGuard guard = new SucursalAccessGuard();

    private CustomUserDetails usuario(Role role, Long sucursalId) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setUsername("test");
        u.setPassword("x");
        u.setNombre("Test");
        u.setRole(role);
        if (sucursalId != null) {
            Sucursal s = new Sucursal();
            s.setId(sucursalId);
            u.setSucursal(s);
        }
        return new CustomUserDetails(u);
    }

    @Test
    @DisplayName("Administrador puede gestionar cualquier sucursal")
    void adminAccedeACualquierSucursal() {
        CustomUserDetails admin = usuario(Role.ADMINISTRADOR, null);
        assertThatCode(() -> guard.verificarGestionSucursal(admin, 99L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Encargado SÍ puede gestionar recursos de su propia sucursal")
    void encargadoAccedeASuSucursal() {
        CustomUserDetails encargado = usuario(Role.ENCARGADO, 5L);
        assertThatCode(() -> guard.verificarGestionSucursal(encargado, 5L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Encargado NO puede gestionar recursos de otra sucursal (403)")
    void encargadoRechazadoEnOtraSucursal() {
        CustomUserDetails encargado = usuario(Role.ENCARGADO, 5L);
        assertThatThrownBy(() -> guard.verificarGestionSucursal(encargado, 6L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Cliente no está autorizado a gestionar sucursales")
    void clienteRechazado() {
        CustomUserDetails cliente = usuario(Role.CLIENTE, null);
        assertThatThrownBy(() -> guard.verificarGestionSucursal(cliente, 1L))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
