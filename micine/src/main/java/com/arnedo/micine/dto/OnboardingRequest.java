package com.arnedo.micine.dto;

import java.util.List;

public class OnboardingRequest {

    private List plataformaIds;
    private List generoIds;

    // Getters y Setters
    public List getPlataformaIds() { return plataformaIds; }
    public void setPlataformaIds(List plataformaIds) { this.plataformaIds = plataformaIds; }

    public List getGeneroIds() { return generoIds; }
    public void setGeneroIds(List generoIds) { this.generoIds = generoIds; }
}