package com.arnedo.micine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FotoPerfilRequest(@NotBlank @Size(max = 87384) String base64) {}
