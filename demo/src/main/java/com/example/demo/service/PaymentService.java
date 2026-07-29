package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${app.public.url:}")
    private String publicUrl;

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public PaymentService(CartService cartService, UserService userService,
                          OrderService orderService, OrderRepository orderRepository) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    /** URL que o Mercado Pago chama quando o pagamento muda de status. */
    private String notificationUrlJsonField() {
        String base = (publicUrl != null && !publicUrl.isBlank()) ? publicUrl : frontendUrl;
        if (base == null || base.isBlank() || base.contains("localhost")) {
            logger.warn("app.public.url / FRONTEND_URL não configurados — webhook MP não será registrado no pagamento");
            return "";
        }
        String url = base.replaceAll("/+$", "") + "/orders/webhook/mp";
        return ",\"notification_url\":\"" + url + "\"";
    }

    public Map<String, Object> createPixPayment(Long userId, Long orderId, String email, String cpf, String firstName, String lastName) throws Exception {
        Double total;
        if (orderId != null) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
            total = order.getTotal();
            if (total == null && order.getSubtotal() != null) {
                total = order.getSubtotal() + (order.getShippingCost() != null ? order.getShippingCost() : 0.0);
            }
            if (total == null) {
                throw new RuntimeException("Total do pedido #" + orderId + " não foi calculado. Verifique se o pedido foi criado corretamente.");
            }
        } else {
            total = cartService.getCartTotal(userId);
        }
        if (total == null || total <= 0) throw new RuntimeException("Carrinho vazio ou valor inválido para pagamento PIX");

        String totalStr = String.format("%.2f", total).replace(",", ".");
        String extRef = orderId != null ? String.valueOf(orderId) : UUID.randomUUID().toString();

        // Exatamente como a doc do MP: POST /v1/orders
        String body = "{"
            + "\"type\":\"online\","
            + "\"total_amount\":\"" + totalStr + "\","
            + "\"external_reference\":\"" + extRef + "\","
            + "\"processing_mode\":\"automatic\""
            + notificationUrlJsonField() + ","
            + "\"payer\":{\"email\":\"" + email + "\"},"
            + "\"transactions\":{\"payments\":[{"
            +   "\"amount\":\"" + totalStr + "\","
            +   "\"payment_method\":{\"id\":\"pix\",\"type\":\"bank_transfer\"}"
            + "}]}"
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadopago.com/v1/orders"))
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> httpResponse = http
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

        // Salva o ID do PAGAMENTO e o CPF do comprador no pedido
        // O MP envia o ID numérico no webhook (ex: "167205876627"), extraído da ticket_url
        String numericPaymentId = extractNumericPaymentId(ticketUrl);
        String paymentId = extractPaymentId(responseBody);
        String idToSave = numericPaymentId != null ? numericPaymentId : (paymentId != null ? paymentId : id);
        final String cleanCpfFinal = cpf != null ? cpf.replaceAll("[^0-9]", "") : null;
        if (orderId != null) {
            final String finalId = idToSave;
            orderRepository.findById(orderId).ifPresent(o -> {
                if (finalId != null) o.setMpPaymentId(finalId);
                if (cleanCpfFinal != null && !cleanCpfFinal.isBlank()) o.setBuyerCpf(cleanCpfFinal);
                orderRepository.save(o);
            });
        }

        // Garante que o total retornado ao frontend nunca seja nulo.
        // O total já foi calculado acima a partir do pedido ou do carrinho e foi
        // enviado corretamente ao Mercado Pago como "total_amount". Se por algum
        // motivo o valor local ainda for nulo, usa o "total_amount" confirmado
        // pela API como fallback definitivo.
        if (total == null || total <= 0) {
            String totalAmountStr = extractJson(responseBody, "total_amount");
            if (totalAmountStr != null) {
                try {
                    total = Double.parseDouble(totalAmountStr.replace(",", "."));
                } catch (NumberFormatException ignored) {
                    // mantém o valor anterior se o parse falhar
                }
            }
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

    private String extractNumericPaymentId(String ticketUrl) {
        if (ticketUrl == null) return null;
        String marker = "/payments/";
        int start = ticketUrl.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        int end = ticketUrl.indexOf("/", start);
        if (end == -1) end = ticketUrl.indexOf("?", start);
        if (end == -1) return null;
        String candidate = ticketUrl.substring(start, end);
        return candidate.matches("\\d+") ? candidate : null;
    }

    private String extractPaymentId(String json) {
        String search = "\"payments\":[{\"id\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }

    /**
     * Extrai string JSON e decodifica escapes (\u0026 → &, etc.).
     * Sem isso a ticket_url do boleto quebra no navegador ("Pagamento não encontrado").
     */
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                if (n == 'u' && i + 5 < json.length()) {
                    try {
                        sb.append((char) Integer.parseInt(json.substring(i + 2, i + 6), 16));
                        i += 5;
                        continue;
                    } catch (NumberFormatException ignored) {
                        // cai no append literal abaixo
                    }
                } else if (n == '"' || n == '\\' || n == '/') {
                    sb.append(n);
                    i++;
                    continue;
                } else if (n == 'n') {
                    sb.append('\n');
                    i++;
                    continue;
                } else if (n == 't') {
                    sb.append('\t');
                    i++;
                    continue;
                }
            }
            if (c == '"') break;
            sb.append(c);
        }
        return sb.toString();
    }

    public Map<String, Object> createBoletoPayment(Long userId, Long orderId, String email, String cpf, String firstName, String lastName) throws Exception {
        Double total;
        String zipCode = "01310-100", streetName = "Av. Paulista", streetNumber = "1000",
               neighborhood = "Bela Vista", city = "São Paulo", state = "SP";

        if (orderId != null) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
            total = order.getTotal();
            if (total == null && order.getSubtotal() != null) {
                total = order.getSubtotal() + (order.getShippingCost() != null ? order.getShippingCost() : 0.0);
            }
            if (total == null) {
                throw new RuntimeException("Total do pedido #" + orderId + " não foi calculado. Verifique se o pedido foi criado corretamente.");
            }
            if (order.getCep() != null) zipCode = order.getCep();
            if (order.getRua() != null) streetName = order.getRua();
            if (order.getNumero() != null) streetNumber = order.getNumero();
            if (order.getBairro() != null) neighborhood = order.getBairro();
            if (order.getCidade() != null) city = order.getCidade();
            if (order.getEstado() != null) state = order.getEstado();
        } else {
            total = cartService.getCartTotal(userId);
        }
        if (total == null || total <= 0) throw new RuntimeException("Carrinho vazio ou valor inválido para pagamento por boleto");

        String totalStr = String.format("%.2f", total).replace(",", ".");
        String cleanCpf = cpf.replaceAll("[^0-9]", "");
        String extRef = orderId != null ? String.valueOf(orderId) : UUID.randomUUID().toString();

        String body = "{"
            + "\"type\":\"online\","
            + "\"total_amount\":\"" + totalStr + "\","
            + "\"external_reference\":\"" + extRef + "\","
            + "\"processing_mode\":\"automatic\""
            + notificationUrlJsonField() + ","
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
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> httpResponse = http
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("=== BOLETO ORDERS API === status=" + httpResponse.statusCode() + " body=" + httpResponse.body());

        if (httpResponse.statusCode() >= 400) {
            throw new RuntimeException(httpResponse.body());
        }

        String responseBody = httpResponse.body();
        String status = extractJson(responseBody, "status");
        String id = extractJson(responseBody, "id");
        String ticketUrl = extractJson(responseBody, "ticket_url");
        String digitableLine = extractJson(responseBody, "digitable_line");
        String barcodeContent = extractJson(responseBody, "barcode_content");

        // Salva o ID numérico do pagamento (webhook MP) — não o ORD...
        String numericPaymentId = extractNumericPaymentId(ticketUrl);
        String idToSave = numericPaymentId != null ? numericPaymentId : id;
        if (orderId != null && idToSave != null) {
            orderRepository.findById(orderId).ifPresent(o -> {
                o.setMpPaymentId(idToSave);
                if (cpf != null && !cpf.isBlank()) {
                    o.setBuyerCpf(cpf.replaceAll("[^0-9]", ""));
                }
                orderRepository.save(o);
            });
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", idToSave != null ? idToSave : id);
        response.put("status", status);
        response.put("boleto_url", ticketUrl);
        response.put("digitable_line", digitableLine);
        response.put("barcode_content", barcodeContent);
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
            if (total == null && order.getSubtotal() != null) {
                total = order.getSubtotal() + (order.getShippingCost() != null ? order.getShippingCost() : 0.0);
            }
            if (total == null) {
                throw new RuntimeException("Total do pedido #" + orderId + " não foi calculado. Verifique se o pedido foi criado corretamente.");
            }
        } else {
            total = cartService.getCartTotal(userId);
        }
        if (total == null || total <= 0) throw new RuntimeException("Carrinho vazio ou valor inválido para pagamento com cartão");

        int inst = installments != null ? installments : 1;
        String totalStr = String.format("%.2f", total).replace(",", ".");
        String extRef = orderId != null ? String.valueOf(orderId) : UUID.randomUUID().toString();

        String body = "{"
            + "\"type\":\"online\","
            + "\"total_amount\":\"" + totalStr + "\","
            + "\"external_reference\":\"" + extRef + "\","
            + "\"processing_mode\":\"automatic\""
            + notificationUrlJsonField() + ","
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
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> httpResponse = http
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

    /**
     * Webhook MP: consulta o pagamento/pedido na API e marca o pedido local como PAID.
     * Aceita formatos clássicos (payment.*) e Orders API (order.* / topic query).
     */
    public void handleMercadoPagoWebhook(Map<String, Object> body, String topic, String typeParam, String idParam) {
        String resourceId = idParam;
        String kind = topic != null ? topic : typeParam;

        if (body != null) {
            if (resourceId == null || resourceId.isBlank()) {
                Object data = body.get("data");
                if (data instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
                    resourceId = dataMap.get("id").toString();
                } else if (body.get("id") != null) {
                    resourceId = body.get("id").toString();
                }
            }
            if (kind == null || kind.isBlank()) {
                Object t = body.get("type");
                if (t == null) t = body.get("topic");
                if (t == null) t = body.get("action");
                if (t != null) kind = t.toString();
            }
        }

        if (resourceId == null || resourceId.isBlank()) {
            logger.warn("Webhook MP sem id de recurso: body={}", body);
            return;
        }

        logger.info("Webhook MP processando resourceId={} kind={}", resourceId, kind);

        // 1) Tenta API de payments clássica
        if (tryMarkFromPaymentApi(resourceId)) {
            // se não marcou PAID, ainda tenta Orders e busca
        }

        // 2) Tenta Orders API (id ORD...)
        tryMarkFromOrdersApi(resourceId);

        // 3) Match direto pelo mpPaymentId já salvo
        orderRepository.findByMpPaymentId(resourceId).ifPresent(o -> {
            if ("PENDING".equals(o.getStatus())) {
                orderService.markAsPaid(o.getId(), resourceId);
                logger.info("Pedido #{} marcado PAID via mpPaymentId direto", o.getId());
            }
        });

        // 4) Se o body/query trouxer external_reference numérico, busca e marca
        String extFromBody = null;
        if (body != null && body.get("external_reference") != null) {
            extFromBody = body.get("external_reference").toString();
        }
        if (extFromBody != null && extFromBody.matches("\\d+")) {
            searchAndMarkByExternalReference(extFromBody);
        }
    }

    private boolean tryMarkFromPaymentApi(String paymentId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/v1/payments/" + paymentId))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                logger.debug("Payment API {} -> {}", paymentId, res.statusCode());
                return false;
            }
            String json = res.body();
            String status = extractJson(json, "status");
            String extRef = extractJson(json, "external_reference");
            logger.info("Payment {} status={} external_reference={}", paymentId, status, extRef);

            if (!isPaidStatus(status)) return true; // encontrado, mas ainda não pago

            Long orderId = resolveOrderId(paymentId, extRef);
            if (orderId != null) {
                try {
                    orderService.markAsPaid(orderId, paymentId);
                    logger.info("Pedido #{} marcado PAID via Payment API", orderId);
                } catch (Exception e) {
                    logger.info("Pedido #{} já processado ou erro: {}", orderId, e.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            logger.warn("Falha ao consultar Payment API {}: {}", paymentId, e.getMessage());
            return false;
        }
    }

    private boolean tryMarkFromOrdersApi(String orderMpId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/v1/orders/" + orderMpId))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                logger.debug("Orders API {} -> {}", orderMpId, res.statusCode());
                return false;
            }
            String json = res.body();
            String status = extractJson(json, "status");
            String extRef = extractJson(json, "external_reference");
            String ticketUrl = extractJson(json, "ticket_url");
            String numericPayId = extractNumericPaymentId(ticketUrl);
            logger.info("Order MP {} status={} external_reference={}", orderMpId, status, extRef);

            if (!isPaidStatus(status)) return true;

            String mpId = numericPayId != null ? numericPayId : orderMpId;
            Long orderId = resolveOrderId(mpId, extRef);
            if (orderId != null) {
                try {
                    orderService.markAsPaid(orderId, mpId);
                    logger.info("Pedido #{} marcado PAID via Orders API", orderId);
                } catch (Exception e) {
                    logger.info("Pedido #{} já processado ou erro: {}", orderId, e.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            logger.warn("Falha ao consultar Orders API {}: {}", orderMpId, e.getMessage());
            return false;
        }
    }

    private Long resolveOrderId(String mpPaymentId, String externalReference) {
        if (mpPaymentId != null) {
            var byMp = orderRepository.findByMpPaymentId(mpPaymentId);
            if (byMp.isPresent()) return byMp.get().getId();
        }
        if (externalReference != null && externalReference.matches("\\d+")) {
            try {
                Long id = Long.parseLong(externalReference);
                if (orderRepository.findById(id).isPresent()) return id;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private boolean isPaidStatus(String status) {
        if (status == null) return false;
        String s = status.toLowerCase();
        return s.equals("approved") || s.equals("paid") || s.equals("processed")
                || s.equals("accredited");
    }

    /**
     * Consulta o Mercado Pago pelo pedido local e marca PAID se o pagamento estiver aprovado.
     * Usado pelo webhook, pela tela do pedido e pelo admin (sem depender só da notificação).
     */
    public Map<String, Object> syncOrderPaymentStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        Map<String, Object> out = new HashMap<>();
        String before = order.getStatus();
        out.put("orderId", orderId);
        out.put("statusBefore", before);

        if (before != null && List.of("PAID", "SHIPPED", "DELIVERED").contains(before)) {
            out.put("status", before);
            out.put("updated", false);
            return out;
        }

        if (order.getMpPaymentId() != null && !order.getMpPaymentId().isBlank()) {
            tryMarkFromPaymentApi(order.getMpPaymentId());
            order = orderRepository.findById(orderId).orElse(order);
            if ("PENDING".equals(order.getStatus())) {
                tryMarkFromOrdersApi(order.getMpPaymentId());
            }
        }

        order = orderRepository.findById(orderId).orElse(order);
        if ("PENDING".equals(order.getStatus())) {
            searchAndMarkByExternalReference(String.valueOf(orderId));
        }

        order = orderRepository.findById(orderId).orElse(order);
        out.put("status", order.getStatus());
        out.put("updated", !before.equals(order.getStatus()));
        return out;
    }

    private void searchAndMarkByExternalReference(String orderIdStr) {
        try {
            String q = URLEncoder.encode(orderIdStr, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/v1/payments/search?external_reference=" + q + "&sort=date_created&criteria=desc"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                logger.warn("Busca MP por external_reference={} -> {}", orderIdStr, res.statusCode());
                return;
            }
            JsonNode root = objectMapper.readTree(res.body());
            JsonNode results = root.path("results");
            if (!results.isArray()) return;

            for (JsonNode payment : results) {
                String status = payment.path("status").asText(null);
                JsonNode idNode = payment.get("id");
                String paymentId = idNode == null || idNode.isNull() ? null : idNode.asText();
                String extRef = payment.path("external_reference").asText(null);
                logger.info("Search MP payment id={} status={} extRef={}", paymentId, status, extRef);
                if (!isPaidStatus(status)) continue;

                Long orderId = resolveOrderId(paymentId, extRef != null ? extRef : orderIdStr);
                if (orderId != null) {
                    try {
                        orderService.markAsPaid(orderId, paymentId != null ? paymentId : orderIdStr);
                        logger.info("Pedido #{} marcado PAID via busca external_reference", orderId);
                    } catch (Exception e) {
                        logger.info("Pedido #{} já processado ou erro: {}", orderId, e.getMessage());
                    }
                    return;
                }
            }
        } catch (Exception e) {
            logger.warn("Falha na busca MP por external_reference {}: {}", orderIdStr, e.getMessage());
        }
    }
}
