package com.uca.pncparcialfinalrestaurante.service;

import com.uca.pncparcialfinalrestaurante.domain.Role;
import com.uca.pncparcialfinalrestaurante.exception.ForbiddenOperationException;
import com.uca.pncparcialfinalrestaurante.security.CustomUserDetails;
import org.springframework.stereotype.Component;

/**
 * ==========================================================================
 *  REGLA DE NEGOCIO B — Autorización por atributo (sucursal), no solo por rol.
 * ==========================================================================
 *
 * <p>El rol {@code ENCARGADO} no basta para autorizar una operación: hay que
 * comparar la <b>sucursal del usuario autenticado</b> contra la <b>sucursal del
 * recurso</b> (mesa o pedido). Esta clase centraliza esa comparación para que la
 * regla se aplique de forma consistente en toda la capa de negocio.
 *
 * <ul>
 *   <li>ADMINISTRADOR: acceso total (cualquier sucursal).</li>
 *   <li>ENCARGADO: solo si su sucursal coincide con la del recurso.</li>
 *   <li>Otros roles: no autorizados por esta vía.</li>
 * </ul>
 */
@Component
public class SucursalAccessGuard {

    /**
     * Verifica que {@code actor} puede gestionar un recurso que pertenece a
     * {@code sucursalIdRecurso}. Lanza {@link ForbiddenOperationException} (HTTP 403)
     * si un encargado intenta operar sobre otra sucursal.
     */
    public void verificarGestionSucursal(CustomUserDetails actor, Long sucursalIdRecurso) {
        Role role = actor.getUsuario().getRole();

        if (role == Role.ADMINISTRADOR) {
            return; // El administrador puede gestionar cualquier sucursal.
        }

        if (role == Role.ENCARGADO) {
            Long sucursalActor = actor.getSucursalId();
            if (sucursalActor == null || !sucursalActor.equals(sucursalIdRecurso)) {
                throw new ForbiddenOperationException(
                        "Un encargado solo puede gestionar recursos de su propia sucursal");
            }
            return;
        }

        throw new ForbiddenOperationException("No tiene permisos para gestionar este recurso");
    }
}
