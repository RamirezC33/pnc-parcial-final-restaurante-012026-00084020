package com.uca.pncparcialfinalrestaurante.exception;

/** Violación de una regla de negocio (datos inválidos, estado incorrecto) -> HTTP 400. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
