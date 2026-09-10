package com.arnedo.micine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmarCuentaRequest(@NotBlank @Size(max = 128) String passwordActual) {}
