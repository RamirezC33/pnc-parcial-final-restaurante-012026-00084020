package com.uca.pncparcialfinalrestaurante.exception;

/**
 * El usuario está autenticado pero NO tiene permiso sobre este recurso concreto
 * -> HTTP 403. La usa la regla de negocio B (un encargado intentando operar
 * pedidos de una sucursal distinta a la suya).
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
