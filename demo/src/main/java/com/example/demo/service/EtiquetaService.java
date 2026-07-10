package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ExternalServiceException;
import com.example.demo.exception.AppException;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class EtiquetaService {

    @Value("${melhorenvio.token}")
    private String token;

    @Value("${melhorenvio.sandbox:false}")
    private boolean sandbox;

    private String meUrl(String path) {
        String base = sandbox
            ? "https://sandbox.melhorenvio.com.br/api/v2"
            : "https://melhorenvio.com.br/api/v2";
        return base + path;
    }

    @Value("${LOJA_NOME}") private String lojaNome;
    @Value("${LOJA_CPF}") private String lojaCpf;
    @Value("${LOJA_TELEFONE}") private String lojaTelefone;
    @Value("${LOJA_EMAIL}") private String lojaEmail;
    @Value("${LOJA_CEP}") private String lojaCep;
    @Value("${LOJA_RUA}") private String lojaRua;
    @Value("${LOJA_NUMERO}") private String lojaNumero;
    @Value("${LOJA_COMPLEMENTO:}") private String lojaComplemento;
    @Value("${LOJA_BAIRRO}") private String lojaBairro;
    @Value("${LOJA_CIDADE}") private String lojaCidade;
    @Value("${LOJA_ESTADO}") private String lojaEstado;
    @Value("${PACOTE_PESO}") private double pacotePeso;
    @Value("${PACOTE_ALTURA}") private int pacoteAltura;
    @Value("${PACOTE_LARGURA}") private int pacoteLargura;
    @Value("${PACOTE_COMPRIMENTO}") private int pacoteComprimento;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gera etiqueta no Melhor Envio para o pedido.
     * Retorna { trackingCode, labelUrl }
     */
    public Map<String, String> gerarEtiqueta(Order order) throws Exception {
        validarPedido(order);

        // 1. Adiciona envio ao carrinho do Melhor Envio
        String cartId = adicionarAoCarrinho(order);

        // 2. Finaliza compra
        checkout(cartId);

        // 3. Gera etiqueta e obtém URL + tracking
        return gerarLabel(cartId);
    }

    private String adicionarAoCarrinho(Order order) throws Exception {
        int serviceId = "SEDEX".equals(order.getShippingMethod()) ? 2 : 1;

        StringBuilder produtos = new StringBuilder();
        if (order.getItems() != null) {
            for (int i = 0; i < order.getItems().size(); i++) {
                OrderItem item = order.getItems().get(i);
                if (i > 0) produtos.append(",");
                produtos.append("{")
                    .append("\"name\":\"").append(escape(item.getProductName())).append("\",")
                    .append("\"quantity\":").append(item.getQuantity()).append(",")
                    .append("\"unitary_value\":").append(item.getProductPrice())
                    .append("}");
            }
        }

        String body = "{"
            + "\"service\":" + serviceId + ","
            + "\"from\":{"
            +   "\"name\":\"" + escape(lojaNome) + "\","
            +   "\"phone\":\"" + lojaTelefone + "\","
            +   "\"email\":\"" + lojaEmail + "\","
            +   "\"document\":\"" + lojaCpf.replaceAll("[^0-9]", "") + "\","
            +   "\"address\":\"" + escape(lojaRua) + "\","
            +   "\"number\":\"" + lojaNumero + "\","
            +   "\"complement\":\"" + escape(lojaComplemento) + "\","
            +   "\"district\":\"" + escape(lojaBairro) + "\","
            +   "\"city\":\"" + escape(lojaCidade) + "\","
            +   "\"state_abbr\":\"" + lojaEstado + "\","
            +   "\"country_id\":\"BR\","
            +   "\"postal_code\":\"" + lojaCep + "\""
            + "},"
            + "\"to\":{"
            +   "\"name\":\"" + escape(order.getUser().getName()) + "\","
            +   "\"email\":\"" + escape(order.getUser().getEmail()) + "\","
            +   "\"document\":\"" + (order.getBuyerCpf() != null ? order.getBuyerCpf() : "") + "\","
            +   "\"address\":\"" + escape(order.getRua()) + "\","
            +   "\"number\":\"" + escape(order.getNumero()) + "\","
            +   "\"complement\":\"" + escape(order.getComplemento() != null ? order.getComplemento() : "") + "\","
            +   "\"district\":\"" + escape(order.getBairro()) + "\","
            +   "\"city\":\"" + escape(order.getCidade()) + "\","
            +   "\"state_abbr\":\"" + order.getEstado() + "\","
            +   "\"country_id\":\"BR\","
            +   "\"postal_code\":\"" + order.getCep().replaceAll("[^0-9]", "") + "\""
            + "},"
            + "\"products\":[" + produtos + "],"
            + "\"volumes\":[{"
            +   "\"height\":" + pacoteAltura + ","
            +   "\"width\":" + pacoteLargura + ","
            +   "\"length\":" + pacoteComprimento + ","
            +   "\"weight\":" + pacotePeso
            + "}],"
            + "\"options\":{"
            +   "\"insurance_value\":" + order.getTotal() + ","
            +   "\"receipt\":false,"
            +   "\"own_hand\":false,"
            +   "\"reverse\":false,"
            +   "\"non_commercial\":false"
            + "}"
            + "}";

        HttpResponse<String> res = post(meUrl("/me/cart"), body);
        System.out.println("=== ME CART === status=" + res.statusCode() + " body=" + res.body());

        if (res.statusCode() >= 400) {
            throw melhorEnvioException("Erro ao criar envio no Melhor Envio", res);
        }

        String cartId = extractJson(res.body(), "id");
        if (cartId == null || cartId.isBlank()) {
            throw new ExternalServiceException("Melhor Envio não retornou o identificador do carrinho para gerar a etiqueta");
        }

        return cartId;
    }

    private void checkout(String cartId) throws Exception {
        String body = "{\"orders\":[\"" + cartId + "\"]}";
        HttpResponse<String> res = post(meUrl("/me/shipment/checkout"), body);
        System.out.println("=== ME CHECKOUT === status=" + res.statusCode() + " body=" + res.body());
        if (res.statusCode() >= 400) {
            throw melhorEnvioException("Erro no checkout do Melhor Envio", res);
        }
    }

    private Map<String, String> gerarLabel(String cartId) throws Exception {
        String body = "{\"orders\":[\"" + cartId + "\"]}";
        HttpResponse<String> res = post(meUrl("/me/shipment/generate"), body);
        System.out.println("=== ME GENERATE === status=" + res.statusCode() + " body=" + res.body());
        if (res.statusCode() >= 400) {
            throw melhorEnvioException("Erro ao gerar etiqueta", res);
        }

        String tracking = extractJson(res.body(), "tracking");
        String labelUrl = extractJson(res.body(), "label");

        // Busca URL de impressão
        HttpResponse<String> printRes = post(meUrl("/me/shipment/print"),
                "{\"mode\":\"private\",\"orders\":[\"" + cartId + "\"]}");
        if (printRes.statusCode() >= 400) {
            throw melhorEnvioException("Erro ao gerar link de impressão da etiqueta", printRes);
        }
        String printUrl = extractJson(printRes.body(), "url");

        if ((tracking == null || tracking.isBlank()) && (labelUrl == null || labelUrl.isBlank()) && (printUrl == null || printUrl.isBlank())) {
            throw new ExternalServiceException("Melhor Envio não retornou código de rastreio nem URL da etiqueta");
        }

        Map<String, String> result = new HashMap<>();
        result.put("trackingCode", tracking);
        result.put("labelUrl", printUrl != null ? printUrl : labelUrl);
        return result;
    }

    private HttpResponse<String> post(String url, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "YoungZone/1.0 (enzosilvaskt7@gmail.com)")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private String extractJson(String json, String key) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode found = findFirstValue(root, key);
            if (found == null || found.isNull()) {
                return null;
            }
            if (found.isValueNode()) {
                return found.asText();
            }
            return found.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void validarPedido(Order order) {
        if (order.getUser() == null) {
            throw new BusinessException("Pedido sem cliente vinculado");
        }
        if (order.getBuyerCpf() == null || order.getBuyerCpf().isBlank()) {
            throw new BusinessException("CPF do comprador é obrigatório para gerar a etiqueta");
        }
        if (order.getCep() == null || order.getCep().isBlank() || order.getRua() == null || order.getRua().isBlank()
                || order.getNumero() == null || order.getNumero().isBlank() || order.getBairro() == null || order.getBairro().isBlank()
                || order.getCidade() == null || order.getCidade().isBlank() || order.getEstado() == null || order.getEstado().isBlank()) {
            throw new BusinessException("Pedido sem endereço completo para gerar a etiqueta");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BusinessException("Pedido sem itens para gerar a etiqueta");
        }
    }

    private AppException melhorEnvioException(String contextMessage, HttpResponse<String> response) {
        String detail = extractErrorMessage(response.body());
        String message = detail == null || detail.isBlank()
                ? contextMessage + " (status " + response.statusCode() + ")"
                : contextMessage + ": " + detail;

        if (response.statusCode() >= 500) {
            return new ExternalServiceException(message);
        }
        return new BusinessException(message);
    }

    private String extractErrorMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode messageNode = findFirstValue(root, "message");
            if (messageNode != null && !messageNode.isNull() && !messageNode.asText().isBlank()) {
                return messageNode.asText();
            }

            JsonNode errorNode = findFirstValue(root, "error");
            if (errorNode != null && !errorNode.isNull() && !errorNode.asText().isBlank()) {
                return errorNode.asText();
            }

            JsonNode errorsNode = findFirstValue(root, "errors");
            if (errorsNode != null && !errorsNode.isNull()) {
                if (errorsNode.isArray() && !errorsNode.isEmpty()) {
                    JsonNode first = errorsNode.get(0);
                    if (first.isTextual()) {
                        return first.asText();
                    }
                    JsonNode firstMessage = findFirstValue(first, "message");
                    if (firstMessage != null && !firstMessage.isNull() && !firstMessage.asText().isBlank()) {
                        return firstMessage.asText();
                    }
                    return first.toString();
                }
                if (errorsNode.isObject()) {
                    JsonNode firstMessage = findFirstValue(errorsNode, "message");
                    if (firstMessage != null && !firstMessage.isNull() && !firstMessage.asText().isBlank()) {
                        return firstMessage.asText();
                    }
                    return errorsNode.toString();
                }
            }
        } catch (Exception ignored) {
            if (json != null && !json.isBlank()) {
                return json;
            }
        }
        return null;
    }

    private JsonNode findFirstValue(JsonNode node, String key) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            JsonNode direct = node.get(key);
            if (direct != null) {
                return direct;
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                JsonNode found = findFirstValue(fields.next().getValue(), key);
                if (found != null) {
                    return found;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                JsonNode found = findFirstValue(item, key);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
