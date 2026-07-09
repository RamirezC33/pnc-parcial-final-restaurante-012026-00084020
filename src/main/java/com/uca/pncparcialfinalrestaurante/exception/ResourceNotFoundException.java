package com.uca.pncparcialfinalrestaurante.exception;

/** Recurso inexistente -> HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
