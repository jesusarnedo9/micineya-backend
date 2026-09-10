package com.arnedo.micine.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RenovarRecomendacionesRequest(
        @NotNull @Size(max = 10) Set<@NotNull @Positive Long> actualesIds
) {}
