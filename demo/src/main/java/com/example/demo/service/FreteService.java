package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FreteService {

    @Value("${melhorenvio.token}")
    private String token;

    @Value("${loja.cep}")
    private String cepOrigem;

    @Value("${melhorenvio.sandbox:false}")
    private boolean sandbox;

    private String meUrl(String path) {
        String base = sandbox
            ? "https://sandbox.melhorenvio.com.br/api/v2"
            : "https://melhorenvio.com.br/api/v2";
        return base + path;
    }

    /**
     * Calcula frete via Melhor Envio.
     * Retorna lista com PAC e SEDEX: [{ service, name, price, days }]
     * Se falhar, retorna fretes fixos por faixa de CEP.
     */
    public List<Map<String, Object>> calcularFrete(String cepDestino, double pesoKg, double altura, double largura, double comprimento) {
        try {
            String cleanDestino = cepDestino.replaceAll("[^0-9]", "");
            String cleanOrigem = cepOrigem.replaceAll("[^0-9]", "");

            String body = "{"
                + "\"from\":{\"postal_code\":\"" + cleanOrigem + "\"},"
                + "\"to\":{\"postal_code\":\"" + cleanDestino + "\"},"
                + "\"package\":{\"height\":" + (int) altura + ",\"width\":" + (int) largura + ",\"length\":" + (int) comprimento + ",\"weight\":" + pesoKg + "},"
                + "\"services\":\"1,2\","   // 1=PAC, 2=SEDEX
                + "\"options\":{\"insurance_value\":0,\"receipt\":false,\"own_hand\":false}"
                + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(meUrl("/me/shipment/calculate")))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "YoungZone/1.0 (contato@youngzone.com.br)")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("=== MELHOR ENVIO === status=" + response.statusCode());

            if (response.statusCode() == 200) {
                return parseMelhorEnvio(response.body());
            }
        } catch (Exception e) {
            System.err.println("Melhor Envio error: " + e.getMessage());
        }

        // Fallback: frete fixo por faixa de CEP
        return freteFallback(cepDestino);
    }

    private List<Map<String, Object>> parseMelhorEnvio(String json) {
        List<Map<String, Object>> result = new ArrayList<>();
        // Extrai cada serviço do array JSON de forma simples
        String[] services = { "PAC", "SEDEX" };
        String[] ids = { "\"id\":1", "\"id\":2" };

        for (int i = 0; i < ids.length; i++) {
            int idx = json.indexOf(ids[i]);
            if (idx == -1) continue;
            String segment = json.substring(idx, Math.min(idx + 500, json.length()));

            String price = extractValue(segment, "price");
            String days = extractValue(segment, "delivery_time");
            String error = extractValue(segment, "error");

            if (error != null) continue; // serviço não disponível para essa rota

            if (price != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("service", services[i]);
                m.put("name", services[i].equals("PAC") ? "PAC — Correios" : "SEDEX — Correios");
                m.put("price", Double.parseDouble(price));
                m.put("days", days != null ? days + " dias úteis" : "—");
                result.add(m);
            }
        }

        if (result.isEmpty()) return freteFallback(null);
        return result;
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        char first = json.charAt(start);
        if (first == '"') {
            int end = json.indexOf('"', start + 1);
            return end == -1 ? null : json.substring(start + 1, end);
        } else if (first == 'n') {
            return null; // null value
        } else {
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
            return json.substring(start, end);
        }
    }

    private List<Map<String, Object>> freteFallback(String cep) {
        double base = 25.0;
        if (cep != null) {
            String digits = cep.replaceAll("[^0-9]", "");
            if (digits.length() >= 5) {
                int prefix = Integer.parseInt(digits.substring(0, 5));
                if (prefix <= 19999) base = 15;
                else if (prefix <= 39999) base = 20;
                else if (prefix <= 65999) base = 25;
                else if (prefix <= 79999) base = 25;
                else if (prefix <= 99999) base = 18;
                else base = 35;
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> pac = new HashMap<>();
        pac.put("service", "PAC");
        pac.put("name", "PAC — Correios");
        pac.put("price", base);
        pac.put("days", "5–8 dias úteis");
        result.add(pac);

        Map<String, Object> sedex = new HashMap<>();
        sedex.put("service", "SEDEX");
        sedex.put("name", "SEDEX — Correios");
        sedex.put("price", base + 15);
        sedex.put("days", "1–3 dias úteis");
        result.add(sedex);

        return result;
    }
}
