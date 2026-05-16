package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public PaymentService(CartService cartService, UserService userService,
                          OrderService orderService, OrderRepository orderRepository) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> createPixPayment(Long userId, Long orderId, String email, String cpf, String firstName, String lastName) throws Exception {
        Double total;
        if (orderId != null) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
            total = order.getTotal();
        } else {
            total = cartService.getCartTotal(userId);
        }
        if (total == null || total <= 0) throw new RuntimeException("Carrinho vazio ou valor inválido");

        String totalStr = String.format("%.2f", total).replace(",", ".");

        // Exatamente como a doc do MP: POST /v1/orders
        String body = "{"
            + "\"type\":\"online\","
            + "\"total_amount\":\"" + totalStr + "\","
            + "\"external_reference\":\"" + UUID.randomUUID() + "\","
            + "\"processing_mode\":\"automatic\","
            + "\"payer\":{\"email\":\"" + email + "\"},"
            + "\"transactions\":{\"payments\":[{"
            +   "\"amount\":\"" + totalStr + "\","
            +   "\"payment_method\":{\"id\":\"pix\",\"type\":\"bank_transfer\"}"
            + "}]}"
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadopago.com/v1/orders"))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> httpResponse = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("=== PIX ORDERS API === status=" + httpResponse.statusCode() + " body=" + httpResponse.body());

        if (httpResponse.statusCode() >= 400) {
            throw new RuntimeException(httpResponse.body());
        }

        // Parse manual da resposta
        String responseBody = httpResponse.body();
        String qrCode = extractJson(responseBody, "qr_code");
        String ticketUrl = extractJson(responseBody, "ticket_url");
        String status = extractJson(responseBody, "status");
        String id = extractJson(responseBody, "id");

        // Salva mpPaymentId no pedido para o webhook encontrar depois
        if (orderId != null && id != null) {
            orderRepository.findById(orderId).ifPresent(o -> {
                o.setMpPaymentId(id);
                orderRepository.save(o);
            });
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("status", status);
        response.put("qr_code", qrCode);
        response.put("ticket_url", ticketUrl);
        response.put("total", total);
        response.put("orderId", orderId);
        return response;
    }

    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }

    public Map<String, Object> createBoletoPayment(Long userId, Long orderId, String email, String cpf, String firstName, String lastName) throws Exception {
        Double total;
        String zipCode = "01310-100", streetName = "Av. Paulista", streetNumber = "1000",
               neighborhood = "Bela Vista", city = "São Paulo", state = "SP";

        if (orderId != null) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
            total = order.getTotal();
            if (order.getCep() != null) zipCode = order.getCep();
            if (order.getRua() != null) streetName = order.getRua();
            if (order.getNumero() != null) streetNumber = order.getNumero();
            if (order.getBairro() != null) neighborhood = order.getBairro();
            if (order.getCidade() != null) city = order.getCidade();
            if (order.getEstado() != null) state = order.getEstado();
        } else {
            total = cartService.getCartTotal(userId);
        }
        if (total == null || total <= 0) throw new RuntimeException("Carrinho vazio ou valor inválido");

        String totalStr = String.format("%.2f", total).replace(",", ".");
        String cleanCpf = cpf.replaceAll("[^0-9]", "");

        String body = "{"
            + "\"type\":\"online\","
            + "\"total_amount\":\"" + totalStr + "\","
            + "\"external_reference\":\"" + UUID.randomUUID() + "\","
            + "\"processing_mode\":\"automatic\","
            + "\"payer\":{"
            +   "\"email\":\"" + email + "\","
            +   "\"first_name\":\"" + firstName + "\","
            +   "\"last_name\":\"" + lastName + "\","
            +   "\"identification\":{\"type\":\"CPF\",\"number\":\"" + cleanCpf + "\"},"
            +   "\"address\":{\"zip_code\":\"" + zipCode + "\",\"street_name\":\"" + streetName + "\",\"street_number\":\"" + streetNumber + "\",\"neighborhood\":\"" + neighborhood + "\",\"city\":\"" + city + "\",\"state\":\"" + state + "\"}},"
            + "\"transactions\":{\"payments\":[{"
            +   "\"amount\":\"" + totalStr + "\","
            +   "\"payment_method\":{\"id\":\"bolbradesco\",\"type\":\"ticket\"}"
            + "}]}"
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadopago.com/v1/orders"))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> httpResponse = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("=== BOLETO ORDERS API === status=" + httpResponse.statusCode() + " body=" + httpResponse.body());

        if (httpResponse.statusCode() >= 400) {
            throw new RuntimeException(httpResponse.body());
        }

        String responseBody = httpResponse.body();
        String status = extractJson(responseBody, "status");
        String id = extractJson(responseBody, "id");
        String ticketUrl = extractJson(responseBody, "ticket_url");

        // Salva mpPaymentId no pedido para o webhook encontrar depois
        if (orderId != null && id != null) {
            orderRepository.findById(orderId).ifPresent(o -> {
                o.setMpPaymentId(id);
                orderRepository.save(o);
            });
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("status", status);
        response.put("boleto_url", ticketUrl);
        response.put("total", total);
        response.put("orderId", orderId);
        return response;
    }

    public Map<String, Object> createCardPayment(Long userId, Long orderId, String email, String token, String paymentMethodId,
                                                   Integer installments, String cpf,
                                                   String firstName, String lastName, String cardType) throws Exception {
        Double total;
        if (orderId != null) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
            total = order.getTotal();
        } else {
            total = cartService.getCartTotal(userId);
        }
        if (total == null || total <= 0) throw new RuntimeException("Carrinho vazio ou valor inválido");

        int inst = installments != null ? installments : 1;
        String totalStr = String.format("%.2f", total).replace(",", ".");

        String body = "{"
            + "\"type\":\"online\","
            + "\"total_amount\":\"" + totalStr + "\","
            + "\"external_reference\":\"" + UUID.randomUUID() + "\","
            + "\"processing_mode\":\"automatic\","
            + "\"payer\":{\"email\":\"" + email + "\","
            +   "\"first_name\":\"" + firstName + "\","
            +   "\"last_name\":\"" + lastName + "\"},"
            + "\"transactions\":{\"payments\":[{"
            +   "\"amount\":\"" + totalStr + "\","
            +   "\"payment_method\":{"
            +     "\"id\":\"" + paymentMethodId + "\","
            +     "\"type\":\"" + cardType + "\","
            +     "\"token\":\"" + token + "\","
            +     "\"installments\":" + inst
            +   "}"
            + "}]}"
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadopago.com/v1/orders"))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> httpResponse = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("=== CARD ORDERS API === status=" + httpResponse.statusCode() + " body=" + httpResponse.body());

        if (httpResponse.statusCode() >= 400) {
            throw new RuntimeException(httpResponse.body());
        }

        String responseBody = httpResponse.body();
        String status = extractJson(responseBody, "status");
        String id = extractJson(responseBody, "id");

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("status", status);
        response.put("total", total);
        response.put("orderId", orderId);

        if (orderId != null && ("paid".equals(status) || "approved".equals(status) || "processed".equals(status))) {
            orderService.markAsPaidWithCpf(orderId, id, cpf);
        }

        return response;
    }
}
