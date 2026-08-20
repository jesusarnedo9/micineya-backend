package com.arnedo.micine.dto;

public class ErrorResponse {
    private String error;
    private int estado;

    public ErrorResponse(String error, int estado) {
        this.error = error;
        this.estado = estado;
    }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public int getEstado() { return estado; }
    public void setEstado(int estado) { this.estado = estado; }
}