package com.example.demo.controller;

import com.example.demo.service.PaymentService;
import com.mercadopago.exceptions.MPApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/public-key")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }

    @PostMapping("/pix")
    public ResponseEntity<?> gerarPix(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
            String email = body.getOrDefault("email", "").toString();
            String cpf = body.getOrDefault("cpf", "19119119100").toString();
            String firstName = body.getOrDefault("firstName", "Cliente").toString();
            String lastName = body.getOrDefault("lastName", "Young Zone").toString();
            return ResponseEntity.ok(paymentService.createPixPayment(userId, orderId, email, cpf, firstName, lastName));
        } catch (MPApiException e) {
            return ResponseEntity.status(400).body(Map.of("erro", e.getMessage(), "detalhes", e.getApiResponse().getContent()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/boleto")
    public ResponseEntity<?> gerarBoleto(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
            String email = body.getOrDefault("email", "").toString();
            String cpf = body.getOrDefault("cpf", "19119119100").toString();
            String firstName = body.getOrDefault("firstName", "Cliente").toString();
            String lastName = body.getOrDefault("lastName", "Young Zone").toString();
            return ResponseEntity.ok(paymentService.createBoletoPayment(userId, orderId, email, cpf, firstName, lastName));
        } catch (MPApiException e) {
            return ResponseEntity.status(400).body(Map.of("erro", e.getMessage(), "detalhes", e.getApiResponse().getContent()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/card")
    public ResponseEntity<?> pagarCartao(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
            Object tokenObj = body.get("token");
            Object methodObj = body.get("paymentMethodId");
            if (tokenObj == null) return ResponseEntity.status(400).body(Map.of("erro", "Token do cartão não recebido"));
            if (methodObj == null) return ResponseEntity.status(400).body(Map.of("erro", "paymentMethodId não recebido"));
            String token = tokenObj.toString();
            String paymentMethodId = methodObj.toString();
            String email = body.getOrDefault("email", "").toString();
            Integer installments = body.containsKey("installments") ? Integer.valueOf(body.get("installments").toString()) : 1;
            String cpf = body.getOrDefault("cpf", "19119119100").toString();
            String firstName = body.getOrDefault("firstName", "Cliente").toString();
            String lastName = body.getOrDefault("lastName", "Young Zone").toString();
            String cardType = body.getOrDefault("cardType", "credit_card").toString();
            return ResponseEntity.ok(paymentService.createCardPayment(userId, orderId, email, token, paymentMethodId, installments, cpf, firstName, lastName, cardType));
        } catch (MPApiException e) {
            return ResponseEntity.status(400).body(Map.of("erro", e.getMessage(), "detalhes", e.getApiResponse().getContent()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erro", e.getMessage()));
        }
    }
}
