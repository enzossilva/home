package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisição de reset de senha.
 */
public class ResetPasswordRequest {
    @NotBlank(message = "Token é obrigatório")
    private String token;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[a-zA-Z\\d@$!%*?&]+$",
        message = "Senha deve conter letras maiúsculas, minúsculas, números e caracteres especiais"
    )
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}

