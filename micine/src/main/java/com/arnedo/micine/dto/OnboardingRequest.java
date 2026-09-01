package com.arnedo.micine.dto;

import java.util.List;

public class OnboardingRequest {

    private List<Long> plataformaIds;
    private List<Long> generoIds;

    // Getters y Setters
    public List<Long> getPlataformaIds() { return plataformaIds; }
    public void setPlataformaIds(List<Long> plataformaIds) { this.plataformaIds = plataformaIds; }

    public List<Long> getGeneroIds() { return generoIds; }
    public void setGeneroIds(List<Long> generoIds) { this.generoIds = generoIds; }
}
