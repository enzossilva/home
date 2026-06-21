package com.example.demo.controller;

import com.example.demo.config.AuthHelper;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final AuthHelper authHelper;

    public OrderController(OrderService orderService, AuthHelper authHelper) {
        this.orderService = orderService;
        this.authHelper = authHelper;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request, HttpServletRequest httpRequest) {
        Long userId = authHelper.getUserId(httpRequest);
        logger.info("Criando novo pedido: userId={}", userId);
        Order order = orderService.createOrder(userId, request);
        return ResponseEntity.status(201).body(ApiResponse.success(order, "Pedido criado com sucesso"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id, HttpServletRequest request) {
        Order order = orderService.getOrder(id);
        authHelper.requireOwnerOrAdmin(request, order.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getOrdersByUser(@PathVariable Long userId, HttpServletRequest request) {
        authHelper.requireOwnerOrAdmin(request, userId);
        logger.info("Listando pedidos do usuário: userId={}", userId);
        List<Order> orders = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, HttpServletRequest request) {
        Long userId = authHelper.getUserId(request);
        logger.info("Cancelando pedido: orderId={}, userId={}", id, userId);
        orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Pedido cancelado com sucesso"));
    }

    @PostMapping("/{id}/set-cpf")
    public ResponseEntity<?> setCpf(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Order order = orderService.getOrder(id);
        authHelper.requireOwnerOrAdmin(request, order.getUser().getId());
        String cpf = body.get("cpf");
        logger.info("Definindo CPF para pedido: orderId={}", id);
        orderService.setBuyerCpf(id, cpf);
        return ResponseEntity.ok(ApiResponse.success(null, "CPF salvo com sucesso"));
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<?> markAsPaid(@PathVariable Long id, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Marcando pedido como pago (admin): orderId={}", id);
        orderService.markAsPaid(id, "admin-manual-" + System.currentTimeMillis());
        return ResponseEntity.ok(ApiResponse.success(null, "Pedido marcado como pago"));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllOrders(HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Admin listando todos os pedidos");
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping("/{id}/etiqueta")
    public ResponseEntity<?> gerarEtiqueta(@PathVariable Long id, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Gerando etiqueta para pedido: orderId={}", id);
        Map<String, String> result = orderService.gerarEtiqueta(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Etiqueta gerada com sucesso"));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<?> markAsShipped(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        String trackingCode = body.get("trackingCode");
        logger.info("Marcando pedido como enviado: orderId={}, tracking={}", id, trackingCode);
        orderService.markAsShipped(id, trackingCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Pedido marcado como enviado"));
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<?> getStats(HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Admin consultando estatísticas");
        Map<String, Object> stats = orderService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/webhook/mp")
    public ResponseEntity<?> webhookMP(@RequestBody Map<String, Object> body) {
        logger.info("Webhook Mercado Pago recebido");
        orderService.processWebhookMP(body);
        // Retorna 200 OK mesmo em erro para evitar retentativas
        return ResponseEntity.ok(ApiResponse.success(null, "Webhook processado"));
    }
}
