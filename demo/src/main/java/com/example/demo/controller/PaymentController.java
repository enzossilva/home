package com.example.demo.controller;

import com.example.demo.config.AuthHelper;
import com.example.demo.dto.ApiResponse;
import com.example.demo.service.PaymentService;
import com.example.demo.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final AuthHelper authHelper;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    public PaymentController(PaymentService paymentService, AuthHelper authHelper) {
        this.paymentService = paymentService;
        this.authHelper = authHelper;
    }

    @GetMapping("/public-key")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("publicKey", publicKey)));
    }

    @PostMapping("/pix")
    public ResponseEntity<?> gerarPix(@RequestBody Map<String, Object> body, HttpServletRequest request) throws Exception {
        Long userId = authHelper.getUserId(request);
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        String email = body.getOrDefault("email", "").toString();
        String cpf = body.getOrDefault("cpf", "").toString();
        String firstName = body.getOrDefault("firstName", "").toString();
        String lastName = body.getOrDefault("lastName", "").toString();

        logger.info("Gerando PIX: userId={}, orderId={}", userId, orderId);

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email é obrigatório", "VALIDATION_ERROR"));
        }
        if (cpf.isBlank() || !SecurityUtils.isValidCPF(cpf)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("CPF inválido", "VALIDATION_ERROR"));
        }

        Map<String, Object> result = paymentService.createPixPayment(userId, orderId, email, cpf, firstName, lastName);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/boleto")
    public ResponseEntity<?> gerarBoleto(@RequestBody Map<String, Object> body, HttpServletRequest request) throws Exception {
        Long userId = authHelper.getUserId(request);
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        String email = body.getOrDefault("email", "").toString();
        String cpf = body.getOrDefault("cpf", "").toString();
        String firstName = body.getOrDefault("firstName", "").toString();
        String lastName = body.getOrDefault("lastName", "").toString();

        logger.info("Gerando Boleto: userId={}, orderId={}", userId, orderId);

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email é obrigatório", "VALIDATION_ERROR"));
        }
        if (cpf.isBlank() || !SecurityUtils.isValidCPF(cpf)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("CPF inválido", "VALIDATION_ERROR"));
        }

        Map<String, Object> result = paymentService.createBoletoPayment(userId, orderId, email, cpf, firstName, lastName);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/card")
    public ResponseEntity<?> pagarCartao(@RequestBody Map<String, Object> body, HttpServletRequest request) throws Exception {
        Long userId = authHelper.getUserId(request);
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        Object tokenObj = body.get("token");
        Object methodObj = body.get("paymentMethodId");

        logger.info("Pagando com cartão: userId={}, orderId={}", userId, orderId);

        if (tokenObj == null || tokenObj.toString().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Token do cartão não recebido", "VALIDATION_ERROR"));
        }
        if (methodObj == null || methodObj.toString().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Payment method não recebido", "VALIDATION_ERROR"));
        }

        String token = tokenObj.toString();
        String paymentMethodId = methodObj.toString();
        String email = body.getOrDefault("email", "").toString();
        Integer installments = body.containsKey("installments") ? Integer.valueOf(body.get("installments").toString()) : 1;
        String cpf = body.getOrDefault("cpf", "").toString();
        String firstName = body.getOrDefault("firstName", "").toString();
        String lastName = body.getOrDefault("lastName", "").toString();
        String cardType = body.getOrDefault("cardType", "credit_card").toString();

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email é obrigatório", "VALIDATION_ERROR"));
        }

        Map<String, Object> result = paymentService.createCardPayment(userId, orderId, email, token, paymentMethodId, installments, cpf, firstName, lastName, cardType);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
