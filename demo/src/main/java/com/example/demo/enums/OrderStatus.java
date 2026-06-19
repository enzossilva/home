package com.example.demo.enums;

/**
 * Status de um pedido no sistema.
 */
public enum OrderStatus {
    PENDING("PENDING", "Aguardando pagamento"),
    PAID("PAID", "Pago - Aguardando envio"),
    SHIPPED("SHIPPED", "Enviado"),
    DELIVERED("DELIVERED", "Entregue"),
    CANCELLED("CANCELLED", "Cancelado");

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status inválido: " + code);
    }

    public boolean canBeCancelled() {
        return this == PENDING || this == PAID;
    }
}

