package com.arnedo.micine.dto;

public class AuthResponse {

    private String token;
    private String refreshToken;
    private String username;

    public AuthResponse(String token, String refreshToken, String username) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
    }

    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public String getUsername() { return username; }
}
