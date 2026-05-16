package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
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

    @Value("${loja.nome}") private String lojaNome;
    @Value("${loja.cpf}") private String lojaCpf;
    @Value("${loja.telefone}") private String lojaTelefone;
    @Value("${loja.email}") private String lojaEmail;
    @Value("${loja.cep}") private String lojaCep;
    @Value("${loja.rua}") private String lojaRua;
    @Value("${loja.numero}") private String lojaNumero;
    @Value("${loja.complemento:}") private String lojaComplemento;
    @Value("${loja.bairro}") private String lojaBairro;
    @Value("${loja.cidade}") private String lojaCidade;
    @Value("${loja.estado}") private String lojaEstado;
    @Value("${loja.pacote.peso}") private double pacotePeso;
    @Value("${loja.pacote.altura}") private int pacoteAltura;
    @Value("${loja.pacote.largura}") private int pacoteLargura;
    @Value("${loja.pacote.comprimento}") private int pacoteComprimento;

    private final HttpClient http = HttpClient.newHttpClient();

    /**
     * Gera etiqueta no Melhor Envio para o pedido.
     * Retorna { trackingCode, labelUrl }
     */
    public Map<String, String> gerarEtiqueta(Order order) throws Exception {
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
            +   "\"document\":\"" + lojaCpf + "\","
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

        if (res.statusCode() >= 400) throw new RuntimeException("Erro ao criar envio no Melhor Envio: " + res.body());

        return extractJson(res.body(), "id");
    }

    private void checkout(String cartId) throws Exception {
        String body = "{\"orders\":[\"" + cartId + "\"]}";
        HttpResponse<String> res = post(meUrl("/me/shipment/checkout"), body);
        System.out.println("=== ME CHECKOUT === status=" + res.statusCode() + " body=" + res.body());
        if (res.statusCode() >= 400) throw new RuntimeException("Erro no checkout Melhor Envio: " + res.body());
    }

    private Map<String, String> gerarLabel(String cartId) throws Exception {
        String body = "{\"orders\":[\"" + cartId + "\"]}";
        HttpResponse<String> res = post(meUrl("/me/shipment/generate"), body);
        System.out.println("=== ME GENERATE === status=" + res.statusCode() + " body=" + res.body());
        if (res.statusCode() >= 400) throw new RuntimeException("Erro ao gerar etiqueta: " + res.body());

        String tracking = extractJson(res.body(), "tracking");
        String labelUrl = extractJson(res.body(), "label");

        // Busca URL de impressão
        HttpResponse<String> printRes = post(meUrl("/me/shipment/print"),
                "{\"mode\":\"private\",\"orders\":[\"" + cartId + "\"]}");
        String printUrl = extractJson(printRes.body(), "url");

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
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
