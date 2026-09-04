package com.arnedo.micine.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("La sesión venció. Volvé a iniciar sesión");
    }
}
