package com.example.demo.enums;

/**
 * Método de envio disponível.
 */
public enum ShippingMethod {
    PAC("PAC", "PAC - Até 10 dias úteis"),
    SEDEX("SEDEX", "SEDEX - Até 2-3 dias úteis");

    private final String code;
    private final String description;

    ShippingMethod(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ShippingMethod fromCode(String code) {
        for (ShippingMethod method : values()) {
            if (method.code.equalsIgnoreCase(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Método de envio inválido: " + code);
    }
}

