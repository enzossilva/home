package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class CreateOrderRequest {
    @NotNull(message = "Endereço é obrigatório")
    private AddressDTO address;

    @NotBlank(message = "Método de envio é obrigatório")
    private String shippingMethod;

    @PositiveOrZero(message = "Custo de envio não pode ser negativo")
    private Double shippingCost;

    public AddressDTO getAddress() { return address; }
    public void setAddress(AddressDTO address) { this.address = address; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public Double getShippingCost() { return shippingCost; }
    public void setShippingCost(Double shippingCost) { this.shippingCost = shippingCost; }
}

