package com.example.demo.enums;

/**
 * Papéis (Roles) dos usuários no sistema.
 */
public enum UserRole {
    CUSTOMER("CUSTOMER", "Cliente"),
    ADMIN("ADMIN", "Administrador");

    private final String code;
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Papel inválido: " + code);
    }
}

