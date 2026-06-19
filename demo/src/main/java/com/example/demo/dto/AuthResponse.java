package com.example.demo.dto;

/**
 * DTO para resposta de autenticação.
 * Nunca expõe a senha do usuário.
 */
public class AuthResponse {
    private UserResponse user;
    private String token;

    public AuthResponse(UserResponse user, String token) {
        this.user = user;
        this.token = token;
    }

    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}

