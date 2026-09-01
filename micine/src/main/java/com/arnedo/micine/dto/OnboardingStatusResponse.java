package com.arnedo.micine.dto;

import java.util.Set;

public record OnboardingStatusResponse(
        String pais,
        Set<Long> plataformaIds,
        Set<Long> generoIds,
        boolean completed
) {
}
