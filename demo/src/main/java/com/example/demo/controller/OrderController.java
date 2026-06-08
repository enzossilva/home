package com.example.demo.controller;

import com.example.demo.config.AuthHelper;
import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthHelper authHelper;

    public OrderController(OrderService orderService, AuthHelper authHelper) {
        this.orderService = orderService;
        this.authHelper = authHelper;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        try {
            Long userId = authHelper.getUserId(request);
            String shippingMethod = body.getOrDefault("shippingMethod", "PAC").toString();

            @SuppressWarnings("unchecked")
            Map<String, String> address = (Map<String, String>) body.get("address");

            double shippingCost = OrderService.calcularFrete(address.get("cep"), shippingMethod);
            Order order = orderService.createOrder(userId, address, shippingCost, shippingMethod);
            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id, HttpServletRequest request) {
        try {
            Order order = orderService.getOrder(id);
            authHelper.requireOwnerOrAdmin(request, order.getUser().getId());
            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getOrdersByUser(@PathVariable Long userId,
                                             HttpServletRequest request) {
        try {
            authHelper.requireOwnerOrAdmin(request, userId);
            return ResponseEntity.ok(orderService.getOrdersByUser(userId));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = authHelper.getUserId(request);
            orderService.cancelOrder(id, userId);
            return ResponseEntity.ok(Map.of("mensagem", "Pedido cancelado"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<?> markAsPaid(@PathVariable Long id) {
        try {
            orderService.markAsPaid(id, "manual-admin");
            return ResponseEntity.ok(Map.of("mensagem", "Pedido marcado como pago"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ── Admin ──────────────────────────────────────────────
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllOrders() {
        try {
            return ResponseEntity.ok(orderService.getAllOrders());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/etiqueta")
    public ResponseEntity<?> gerarEtiqueta(@PathVariable Long id) {
        try {
            Map<String, String> result = orderService.gerarEtiqueta(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<?> markAsShipped(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        try {
            String trackingCode = body.get("trackingCode").toString();
            orderService.markAsShipped(id, trackingCode);
            return ResponseEntity.ok(Map.of("mensagem", "Pedido marcado como enviado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<?> getStats() {
        try {
            List<Order> all = orderService.getAllOrders();
            long today = all.stream()
                    .filter(o -> o.getCreatedAt() != null &&
                            o.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()))
                    .count();
            double receita = all.stream()
                    .filter(o -> "PAID".equals(o.getStatus()) || "SHIPPED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus()))
                    .mapToDouble(o -> o.getTotal() != null ? o.getTotal() : 0)
                    .sum();
            long pagos = all.stream().filter(o -> "PAID".equals(o.getStatus()) || "SHIPPED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus())).count();
            double ticket = pagos > 0 ? receita / pagos : 0;
            long pendentes = all.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
            long aguardandoEnvio = all.stream().filter(o -> "PAID".equals(o.getStatus())).count();

            return ResponseEntity.ok(Map.of(
                    "totalPedidos", all.size(),
                    "pedidosHoje", today,
                    "receitaTotal", receita,
                    "ticketMedio", ticket,
                    "pendentes", pendentes,
                    "aguardandoEnvio", aguardandoEnvio
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ── Webhook Mercado Pago ────────────────────────────────
    @PostMapping("/webhook/mp")
    public ResponseEntity<?> webhookMP(@RequestBody Map<String, Object> body) {
        try {
            String action = body.getOrDefault("action", "").toString();
            if ("payment.updated".equals(action) || "payment.created".equals(action)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null) {
                    String mpId = data.get("id").toString();
                    orderService.getAllOrders().stream()
                            .filter(o -> mpId.equals(o.getMpPaymentId()) && "PENDING".equals(o.getStatus()))
                            .findFirst()
                            .ifPresent(o -> orderService.markAsPaid(o.getId(), mpId));
                }
            }
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "ok"));
        }
    }
}
