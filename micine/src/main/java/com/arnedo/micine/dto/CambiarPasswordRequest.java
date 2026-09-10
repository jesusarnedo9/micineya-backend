package com.arnedo.micine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank @Size(max = 128) String passwordActual,
        @NotBlank @Size(min = 8, max = 72) String passwordNueva
) {}
