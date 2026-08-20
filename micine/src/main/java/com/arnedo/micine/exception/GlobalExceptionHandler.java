package com.arnedo.micine.exception;

import com.arnedo.micine.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Atrapa nuestros "throw new IllegalArgumentException"
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity manejarArgumentosInvalidos(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    // Atrapa cualquier otro error inesperado (como un fallo de base de datos)
    @ExceptionHandler(Exception.class)
    public ResponseEntity manejarErroresGenerales(Exception ex) {
        ErrorResponse error = new ErrorResponse("Ocurrió un error interno en el servidor", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}