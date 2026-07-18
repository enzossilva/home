package com.example.demo.service;

import com.example.demo.exception.ExternalServiceException;
import com.example.demo.model.Order;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Integração direta com a API dos Correios.
 *
 * Por ora o foco é a autenticação (obtenção/renovação do token). Os métodos
 * de criação de postagem e cálculo de frete são esqueletos que serão
 * detalhados em iterações futuras.
 */
@Service
public class CorreiosService {

    private static final String AUTH_URL = "https://api.correios.com.br/token/v1/autentica";

    // Renova o token 30 minutos antes de expirar
    private static final long RENEW_MARGIN_MINUTES = 30;

    @Value("${correios.username}")
    private String username;

    @Value("${correios.access-code}")
    private String accessCode;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedToken;
    private ZonedDateTime tokenExpiraEm;

    /**
     * Retorna um token válido dos Correios, buscando um novo caso o
     * atual esteja ausente, expirado ou próximo de expirar.
     */
    public synchronized String getAuthToken() {
        if (isTokenValid()) {
            return cachedToken;
        }
        autenticar();
        return cachedToken;
    }

    private boolean isTokenValid() {
        if (cachedToken == null || tokenExpiraEm == null) {
            return false;
        }
        ZonedDateTime renewAt = tokenExpiraEm.minus(RENEW_MARGIN_MINUTES, ChronoUnit.MINUTES);
        return ZonedDateTime.now(tokenExpiraEm.getZone()).isBefore(renewAt);
    }

    private void autenticar() {
        try {
            String credentials = username + ":" + accessCode;
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_URL))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + basicAuth)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new ExternalServiceException(
                        "Falha ao autenticar na API dos Correios (status " + response.statusCode() + ")");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode tokenNode = root.get("token");
            JsonNode expiraEmNode = root.get("expiraEm");

            if (tokenNode == null || tokenNode.isNull() || tokenNode.asText().isBlank()) {
                throw new ExternalServiceException("API dos Correios não retornou um token válido");
            }

            cachedToken = tokenNode.asText();
            tokenExpiraEm = parseExpiraEm(expiraEmNode != null ? expiraEmNode.asText() : null);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Erro ao autenticar na API dos Correios: " + e.getMessage(), e);
        }
    }

    private ZonedDateTime parseExpiraEm(String expiraEm) {
        if (expiraEm == null || expiraEm.isBlank()) {
            // Sem informação de expiração: assume 3 horas de validade (padrão dos Correios)
            return ZonedDateTime.now().plusHours(3);
        }
        try {
            return OffsetDateTime.parse(expiraEm, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toZonedDateTime();
        } catch (Exception e) {
            try {
                return ZonedDateTime.parse(expiraEm);
            } catch (Exception ignored) {
                return ZonedDateTime.now().plusHours(3);
            }
        }
    }

    /**
     * Cria uma postagem (pré-postagem) para o pedido nos Correios.
     * TODO: implementar chamada real ao endpoint de pré-postagem/etiquetas dos Correios.
     */
    public Map<String, String> criarPostagem(Order order) {
        // Garante que temos um token válido antes de seguir com a integração
        getAuthToken();

        Map<String, String> result = new HashMap<>();
        result.put("trackingCode", null);
        result.put("labelUrl", null);
        return result;
    }

    /**
     * Calcula o frete entre dois CEPs usando a API dos Correios.
     * TODO: implementar chamada real ao endpoint de cálculo de preço/prazo dos Correios.
     */
    public Map<String, Object> calcularFrete(String cepOrigem, String cepDestino) {
        // Garante que temos um token válido antes de seguir com a integração
        getAuthToken();

        Map<String, Object> result = new HashMap<>();
        result.put("cepOrigem", cepOrigem);
        result.put("cepDestino", cepDestino);
        return result;
    }
}
