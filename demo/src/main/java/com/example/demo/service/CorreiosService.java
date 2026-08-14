package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ExternalServiceException;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integração com a API oficial dos Correios (Token + Pré-postagem + rótulo PDF).
 *
 * Requer contrato comercial + cartão de postagem. Credenciais no Railway.
 */
@Service
public class CorreiosService {
    private static final Logger logger = LoggerFactory.getLogger(CorreiosService.class);

    private static final long RENEW_MARGIN_MINUTES = 30;
    private static final int PDF_POLL_ATTEMPTS = 20;
    private static final long PDF_POLL_SLEEP_MS = 1500;

    @Value("${correios.username:}")
    private String username;

    @Value("${correios.access-code:}")
    private String accessCode;

    @Value("${correios.cartao-postagem:}")
    private String cartaoPostagem;

    @Value("${correios.contrato:}")
    private String contrato;

    @Value("${correios.dr:}")
    private String dr;

    /** prod | hom — default prod (Railway). */
    @Value("${correios.ambiente:prod}")
    private String ambiente;

    /** Códigos oficiais do contrato (confirmar no CWS). */
    @Value("${correios.codigo-pac:03298}")
    private String codigoPac;

    @Value("${correios.codigo-sedex:03220}")
    private String codigoSedex;

    @Value("${loja.nome:}")
    private String lojaNome;

    @Value("${loja.cpf:}")
    private String lojaCpf;

    @Value("${loja.telefone:}")
    private String lojaTelefone;

    @Value("${loja.email:}")
    private String lojaEmail;

    @Value("${loja.cep:}")
    private String lojaCep;

    @Value("${loja.rua:}")
    private String lojaRua;

    @Value("${loja.numero:}")
    private String lojaNumero;

    @Value("${loja.complemento:}")
    private String lojaComplemento;

    @Value("${loja.bairro:}")
    private String lojaBairro;

    @Value("${loja.cidade:}")
    private String lojaCidade;

    @Value("${loja.estado:}")
    private String lojaEstado;

    @Value("${loja.pacote.peso:0.5}")
    private double pacotePesoKg;

    @Value("${loja.pacote.altura:10}")
    private int pacoteAltura;

    @Value("${loja.pacote.largura:15}")
    private int pacoteLargura;

    @Value("${loja.pacote.comprimento:20}")
    private int pacoteComprimento;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedToken;
    private ZonedDateTime tokenExpiraEm;
    /** basico | cartao — escopo do token em cache */
    private String tokenEscopo;

    private String apiHost() {
        return "prod".equalsIgnoreCase(trim(ambiente))
                ? "https://api.correios.com.br"
                : "https://apihom.correios.com.br";
    }

    /** Token com cartão (pré-postagem / etiqueta). */
    public synchronized String getAuthToken() {
        return obterToken("cartao");
    }

    /** Token só com usuário+código (Preço/Prazo — frete). */
    public synchronized String getAuthTokenBasico() {
        return obterToken("basico");
    }

    private String obterToken(String escopo) {
        if (isTokenValid() && escopo.equals(tokenEscopo)) {
            return cachedToken;
        }
        if ("cartao".equals(escopo)) {
            autenticarComCartao();
        } else {
            autenticarBasico();
        }
        return cachedToken;
    }

    private boolean isTokenValid() {
        if (cachedToken == null || tokenExpiraEm == null) {
            return false;
        }
        ZonedDateTime renewAt = tokenExpiraEm.minus(RENEW_MARGIN_MINUTES, ChronoUnit.MINUTES);
        return ZonedDateTime.now(tokenExpiraEm.getZone()).isBefore(renewAt);
    }

    private void autenticarBasico() {
        requireAuthCredentials();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiHost() + "/token/v1/autentica"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic " + basicAuthHeader())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            aplicarRespostaToken(http.send(request, HttpResponse.BodyHandlers.ofString()), "basico");
        } catch (ExternalServiceException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Erro ao autenticar na API dos Correios: " + e.getMessage(), e);
        }
    }

    private void autenticarComCartao() {
        requireConfigured();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("numero", digitsOnly(cartaoPostagem));
            String contratoTrim = trim(contrato);
            if (!isBlank(contratoTrim)) {
                body.put("contrato", digitsOnly(contratoTrim));
            }
            String drTrim = trim(dr);
            if (!isBlank(drTrim)) {
                try {
                    body.put("dr", Integer.parseInt(drTrim));
                } catch (NumberFormatException ignored) {
                    logger.warn("CORREIOS_DR inválido, ignorando: {}", dr);
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiHost() + "/token/v1/autentica/cartaopostagem"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + basicAuthHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            aplicarRespostaToken(http.send(request, HttpResponse.BodyHandlers.ofString()), "cartao");
        } catch (ExternalServiceException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Erro ao autenticar na API dos Correios: " + e.getMessage(), e);
        }
    }

    private String basicAuthHeader() {
        String user = sanitizeCredential(username);
        String pass = sanitizeCredential(accessCode);
        String credentials = user + ":" + pass;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /** Remove aspas/BOM/espaços que o Railway às vezes cola junto com o valor. */
    private static String sanitizeCredential(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("\uFEFF")) s = s.substring(1).trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private void aplicarRespostaToken(HttpResponse<String> response, String escopo) throws Exception {
        if (response.statusCode() >= 400) {
            String user = sanitizeCredential(username);
            String pass = sanitizeCredential(accessCode);
            logger.warn(
                    "Correios auth falhou status={} escopo={} ambiente={} userLen={} userHint={} codeLen={} codePrefix={}",
                    response.statusCode(),
                    escopo,
                    trim(ambiente),
                    user.length(),
                    maskHint(user),
                    pass.length(),
                    pass.length() >= 3 ? pass.substring(0, 3) : "(curto)");

            String hint = response.statusCode() == 401
                    ? " Verifique CORREIOS_USERNAME (usuário Meu Correios), CORREIOS_ACCESS_CODE (código CWS, não a senha do site) e CORREIOS_AMBIENTE=prod."
                    : "";
            if ("cartao".equals(escopo) && (response.statusCode() == 400 || response.statusCode() == 403)) {
                hint += " Confira CORREIOS_CARTAO_POSTAGEM (e remova CORREIOS_CONTRATO/DR se estiverem errados).";
            }
            throw new ExternalServiceException(
                    "Falha ao autenticar nos Correios (status " + response.statusCode() + " ambiente="
                            + trim(ambiente) + "): " + truncate(response.body()) + hint);
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode tokenNode = root.get("token");
        if (tokenNode == null || tokenNode.asText().isBlank()) {
            throw new ExternalServiceException("API dos Correios não retornou token");
        }
        cachedToken = tokenNode.asText();
        tokenExpiraEm = parseExpiraEm(root.path("expiraEm").asText(null));
        tokenEscopo = escopo;
        logger.info("Token Correios obtido escopo={} ambiente={} expiraEm={} apis={}",
                escopo, ambiente, tokenExpiraEm, summarizeApis(root.get("apis")));
    }

    private static String summarizeApis(JsonNode apis) {
        if (apis == null || !apis.isArray() || apis.isEmpty()) return "(nenhuma/ausente)";
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (JsonNode a : apis) {
            if (n > 0) sb.append(',');
            if (a.isTextual()) sb.append(a.asText());
            else if (a.has("api")) sb.append(a.get("api").asText());
            else if (a.has("nome")) sb.append(a.get("nome").asText());
            else if (a.has("id")) sb.append(a.get("id").asText());
            else sb.append(a.toString());
            if (++n >= 20) {
                sb.append(",…");
                break;
            }
        }
        return sb.toString();
    }

    private static String maskHint(String user) {
        if (user == null || user.isBlank()) return "(vazio)";
        if (user.length() <= 4) return "****";
        return user.substring(0, 2) + "…" + user.substring(user.length() - 2);
    }

    /**
     * Cria pré-postagem + solicita rótulo PDF. Retorna trackingCode e labelUrl (data URI).
     */
    public Map<String, String> criarPostagem(Order order) {
        validarPedido(order);
        validarLoja();
        String token = getAuthToken();

        try {
            String prePostagemId = criarPrePostagem(order, token);
            String tracking = extrairCodigoObjeto(prePostagemId, token);
            String labelUrl = gerarRotuloPdf(prePostagemId, token);

            // Após emitir rótulo o código do objeto costuma estar disponível
            if (tracking == null || tracking.isBlank()) {
                tracking = extrairCodigoObjeto(prePostagemId, token);
            }
            if (tracking == null || tracking.isBlank()) {
                throw new ExternalServiceException(
                        "Pré-postagem criada (" + prePostagemId + ") mas sem código de rastreio. Tente novamente.");
            }

            Map<String, String> result = new HashMap<>();
            result.put("trackingCode", tracking);
            result.put("labelUrl", labelUrl);
            result.put("prePostagemId", prePostagemId);
            return result;
        } catch (BusinessException | ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Erro na pré-postagem Correios: " + e.getMessage(), e);
        }
    }

    private String criarPrePostagem(Order order, String token) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("codigoServico", codigoServico(order.getShippingMethod()));
        body.put("pesoInformado", String.valueOf(Math.max(1, Math.round(pacotePesoKg * 1000))));
        body.put("codigoFormatoObjetoInformado", "2"); // caixa/pacote
        body.put("alturaInformada", String.valueOf(pacoteAltura));
        body.put("larguraInformada", String.valueOf(pacoteLargura));
        body.put("comprimentoInformado", String.valueOf(pacoteComprimento));
        body.put("cienteObjetoNaoProibido", "1");
        body.put("solicitarColeta", "N");
        body.put("logisticaReversa", "N");

        ObjectNode remetente = body.putObject("remetente");
        preencherPessoa(remetente, lojaNome, lojaCpf, lojaCep, lojaRua, lojaNumero,
                lojaComplemento, lojaBairro, lojaCidade, lojaEstado, lojaEmail, lojaTelefone);

        String destNome = order.getUser() != null && order.getUser().getName() != null
                ? order.getUser().getName()
                : "Cliente YoungsZone";
        String destEmail = order.getUser() != null ? order.getUser().getEmail() : null;
        ObjectNode destinatario = body.putObject("destinatario");
        preencherPessoa(destinatario, destNome, order.getBuyerCpf(), order.getCep(), order.getRua(),
                order.getNumero(), order.getComplemento(), order.getBairro(), order.getCidade(),
                order.getEstado(), destEmail, null);

        ArrayNode itens = body.putArray("itensDeclaracaoConteudo");
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                ObjectNode linha = itens.addObject();
                linha.put("conteudo", truncate(safe(item.getProductName()), 60));
                linha.put("quantidade", String.valueOf(item.getQuantity() != null ? item.getQuantity() : 1));
                double valor = item.getProductPrice() != null ? item.getProductPrice() : 0;
                linha.put("valor", String.format(java.util.Locale.US, "%.2f", valor));
                int pesoItem = Math.max(1, (int) Math.round((pacotePesoKg * 1000)
                        / Math.max(1, order.getItems().size())));
                linha.put("peso", String.valueOf(pesoItem));
            }
        }
        if (itens.isEmpty()) {
            ObjectNode linha = itens.addObject();
            linha.put("conteudo", "Mercadoria YoungsZone");
            linha.put("quantidade", "1");
            linha.put("valor", String.format(java.util.Locale.US, "%.2f",
                    order.getSubtotal() != null ? order.getSubtotal() : 0));
            linha.put("peso", String.valueOf(Math.max(1, Math.round(pacotePesoKg * 1000))));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiHost() + "/prepostagem/v1/prepostagens"))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ExternalServiceException(
                    "Erro ao criar pré-postagem (status " + response.statusCode() + "): "
                            + truncate(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        String id = firstText(root, "id", "idPrePostagem");
        if (id == null || id.isBlank()) {
            throw new ExternalServiceException("Pré-postagem sem id na resposta: " + truncate(response.body()));
        }
        logger.info("Pré-postagem criada id={} orderId={}", id, order.getId());
        return id;
    }

    private String gerarRotuloPdf(String prePostagemId, String token) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode ids = body.putArray("idsPrePostagem");
        ids.add(prePostagemId);
        body.put("tipoRotulo", "P");
        body.put("formatoRotulo", "ET");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiHost() + "/prepostagem/v1/prepostagens/rotulo/assincrono/pdf"))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ExternalServiceException(
                    "Erro ao solicitar rótulo PDF (status " + response.statusCode() + "): "
                            + truncate(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        String idRecibo = firstText(root, "idRecibo", "id", "recibo");
        if (idRecibo == null || idRecibo.isBlank()) {
            // Algumas respostas já trazem o PDF
            String b64 = firstText(root, "dados", "pdf", "arquivo", "base64");
            if (b64 != null && !b64.isBlank()) {
                return toDataUri(b64);
            }
            throw new ExternalServiceException("Correios não retornaram idRecibo do rótulo: " + truncate(response.body()));
        }

        for (int i = 0; i < PDF_POLL_ATTEMPTS; i++) {
            Thread.sleep(PDF_POLL_SLEEP_MS);
            String pdfB64 = baixarRotuloPdf(idRecibo, token);
            if (pdfB64 != null && !pdfB64.isBlank()) {
                return toDataUri(pdfB64);
            }
        }
        throw new ExternalServiceException("Timeout aguardando PDF do rótulo Correios (recibo " + idRecibo + ")");
    }

    private String baixarRotuloPdf(String idRecibo, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiHost() + "/prepostagem/v1/prepostagens/rotulo/download/assincrono/" + idRecibo))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 204 || response.statusCode() == 202) {
            return null; // ainda processando
        }
        if (response.statusCode() >= 400) {
            logger.debug("Download rótulo ainda não pronto status={} body={}",
                    response.statusCode(), truncate(response.body()));
            return null;
        }

        String raw = response.body();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        // Pode vir JSON ou base64 puro
        if (raw.trim().startsWith("{")) {
            JsonNode root = objectMapper.readTree(raw);
            String b64 = firstText(root, "dados", "pdf", "arquivo", "base64", "conteudo");
            if (b64 != null) return stripDataUri(b64);
            JsonNode arr = root.get("dados");
            if (arr != null && arr.isArray() && !arr.isEmpty()) {
                return stripDataUri(arr.get(0).asText());
            }
            return null;
        }
        return stripDataUri(raw.trim().replace("\"", ""));
    }

    private String extrairCodigoObjeto(String prePostagemId, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiHost() + "/prepostagem/v1/prepostagens/" + prePostagemId))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                // fallback: listagem
                return extrairCodigoObjetoViaLista(prePostagemId, token);
            }
            JsonNode root = objectMapper.readTree(response.body());
            return firstText(root, "codigoObjeto", "codigo_objeto");
        } catch (Exception e) {
            logger.warn("Falha ao consultar código do objeto: {}", e.getMessage());
            return null;
        }
    }

    private String extrairCodigoObjetoViaLista(String prePostagemId, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiHost() + "/prepostagem/v2/prepostagens?id=" + prePostagemId + "&page=0&size=1"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) return null;
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode itens = root.get("itens");
            if (itens == null) itens = root.get("content");
            if (itens != null && itens.isArray() && !itens.isEmpty()) {
                return firstText(itens.get(0), "codigoObjeto", "codigo_objeto");
            }
            return firstText(root, "codigoObjeto");
        } catch (Exception e) {
            return null;
        }
    }

    private void preencherPessoa(ObjectNode node, String nome, String cpfCnpj, String cep, String rua,
                                 String numero, String complemento, String bairro, String cidade,
                                 String uf, String email, String telefone) {
        node.put("nome", safe(nome));
        if (cpfCnpj != null && !cpfCnpj.isBlank()) {
            node.put("cpfCnpj", digitsOnly(cpfCnpj));
        }
        node.put("cep", digitsOnly(cep));
        node.put("logradouro", truncate(safe(rua), 50));
        node.put("numero", truncate(safe(numero), 6));
        if (complemento != null && !complemento.isBlank()) {
            node.put("complemento", truncate(complemento, 30));
        }
        node.put("bairro", truncate(safe(bairro), 50));
        node.put("cidade", truncate(safe(cidade), 50));
        node.put("uf", safe(uf).toUpperCase());
        if (email != null && !email.isBlank()) {
            node.put("email", email.trim());
        }
        String tel = digitsOnly(telefone);
        if (tel.length() >= 10) {
            node.put("dddTelefone", tel.substring(0, 2));
            node.put("telefone", tel.substring(2, Math.min(tel.length(), 10)));
        }
        if (tel.length() >= 11) {
            node.put("dddCelular", tel.substring(0, 2));
            node.put("celular", tel.substring(2, 11));
        }
    }

    private String codigoServico(String shippingMethod) {
        if (shippingMethod != null && shippingMethod.equalsIgnoreCase("SEDEX")) {
            return codigoSedex;
        }
        return codigoPac;
    }

    /** Credenciais mínimas para chamar APIs (token + cartão). */
    public boolean isConfigured() {
        return hasAuthCredentials() && !isBlank(trim(cartaoPostagem));
    }

    public boolean hasAuthCredentials() {
        return !isBlank(trim(username)) && !isBlank(trim(accessCode));
    }

    /**
     * Cota PAC e SEDEX na API de Preço/Prazo dos Correios.
     * Origem = LOJA_CEP. Sem fallback de tabela — exige API configurada e disponível.
     */
    public List<Map<String, Object>> cotarOpcoesFrete(String cepDestino) {
        String dest = digitsOnly(cepDestino);
        if (dest.length() != 8) {
            throw new BusinessException("CEP de destino inválido");
        }

        if (!isConfigured() || isBlank(lojaCep) || digitsOnly(lojaCep).length() != 8) {
            throw new BusinessException(
                    "Frete indisponível: configure CORREIOS_USERNAME, CORREIOS_ACCESS_CODE, CORREIOS_CARTAO_POSTAGEM e LOJA_CEP");
        }

        try {
            // Preço/Prazo costumam exigir token com escopo de cartão/contrato (não só /autentica).
            String token = getAuthToken();
            String origem = digitsOnly(lojaCep);
            int pesoG = Math.max(1, (int) Math.round(pacotePesoKg * 1000));

            List<Map<String, Object>> opts = new ArrayList<>();
            opts.add(cotarServico("PAC", "PAC — Correios", codigoPac, origem, dest, pesoG, token));
            opts.add(cotarServico("SEDEX", "SEDEX — Correios", codigoSedex, origem, dest, pesoG, token));
            return opts;
        } catch (BusinessException | ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Falha na cotação Correios: {}", e.getMessage());
            throw new ExternalServiceException(
                    "Não foi possível cotar frete nos Correios. Tente novamente.", e);
        }
    }

    /** Valor único para gravar no pedido (recalcula no servidor via API). */
    public double cotarValor(String cepDestino, String method) {
        List<Map<String, Object>> opts = cotarOpcoesFrete(cepDestino);
        String want = method == null ? "PAC" : method.toUpperCase();
        for (Map<String, Object> o : opts) {
            if (want.equals(String.valueOf(o.get("service")))) {
                Object p = o.get("price");
                if (p instanceof Number) return ((Number) p).doubleValue();
            }
        }
        throw new BusinessException("Modalidade de frete indisponível: " + want);
    }

    private Map<String, Object> cotarServico(String service, String name, String codigo,
                                              String cepOrigem, String cepDestino,
                                              int pesoG, String token) throws Exception {
        double preco = consultarPreco(codigo, cepOrigem, cepDestino, pesoG, token);
        int dias = consultarPrazoDias(codigo, cepOrigem, cepDestino, token);

        Map<String, Object> opt = new HashMap<>();
        opt.put("service", service);
        opt.put("name", name);
        opt.put("price", preco);
        opt.put("days", formatarPrazo(service, dias));
        return opt;
    }

    private double consultarPreco(String codigoServico, String cepOrigem, String cepDestino,
                                  int pesoG, String token) throws Exception {
        String q = "cepOrigem=" + cepOrigem
                + "&cepDestino=" + cepDestino
                + "&psObjeto=" + pesoG
                + "&tpObjeto=2"
                + "&comprimento=" + pacoteComprimento
                + "&largura=" + pacoteLargura
                + "&altura=" + pacoteAltura;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiHost() + "/preco/v1/nacional/" + codigoServico + "?" + q))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ExternalServiceException(
                    "Preço Correios " + codigoServico + " status " + response.statusCode() + ": "
                            + truncate(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        String pc = firstText(root, "pcFinal", "pcProduto", "pcFinalServico");
        if (pc == null || pc.isBlank()) {
            throw new ExternalServiceException("Resposta de preço sem pcFinal: " + truncate(response.body()));
        }
        return parseReais(pc);
    }

    private int consultarPrazoDias(String codigoServico, String cepOrigem, String cepDestino,
                                   String token) {
        try {
            String q = "cepOrigem=" + cepOrigem + "&cepDestino=" + cepDestino;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiHost() + "/prazo/v1/nacional/" + codigoServico + "?" + q))
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return -1;
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("prazoEntrega") && root.get("prazoEntrega").canConvertToInt()) {
                return root.get("prazoEntrega").asInt();
            }
            String txt = firstText(root, "prazoEntrega", "prazo");
            if (txt != null) {
                return Integer.parseInt(txt.replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            logger.debug("Prazo Correios indisponível: {}", e.getMessage());
        }
        return -1;
    }

    private static String formatarPrazo(String service, int dias) {
        if (dias > 0) {
            return dias == 1 ? "1 dia útil" : dias + " dias úteis";
        }
        return "SEDEX".equalsIgnoreCase(service) ? "1–3 dias úteis" : "5–10 dias úteis";
    }

    private static double parseReais(String valor) {
        String n = valor.trim().replace("R$", "").replace(" ", "");
        // 1.234,56 → 1234.56  |  19,92 → 19.92  |  19.92 → 19.92
        if (n.contains(",") && n.contains(".")) {
            n = n.replace(".", "").replace(",", ".");
        } else if (n.contains(",")) {
            n = n.replace(",", ".");
        }
        return Double.parseDouble(n);
    }

    private void requireAuthCredentials() {
        if (!hasAuthCredentials()) {
            throw new BusinessException(
                    "Correios sem credenciais. Defina CORREIOS_USERNAME e CORREIOS_ACCESS_CODE no Railway.");
        }
    }

    private void requireConfigured() {
        requireAuthCredentials();
        if (isBlank(trim(cartaoPostagem))) {
            throw new BusinessException(
                    "Correios sem cartão. Defina CORREIOS_CARTAO_POSTAGEM no Railway (após abrir contrato + cartão de postagem).");
        }
    }

    private void validarLoja() {
        if (isBlank(lojaNome) || isBlank(lojaCpf) || isBlank(lojaCep) || isBlank(lojaRua)
                || isBlank(lojaNumero) || isBlank(lojaBairro) || isBlank(lojaCidade) || isBlank(lojaEstado)) {
            throw new BusinessException(
                    "Dados da loja incompletos. Configure LOJA_NOME, LOJA_CPF, endereço (LOJA_CEP, LOJA_RUA, etc.) no Railway.");
        }
    }

    private void validarPedido(Order order) {
        if (order.getBuyerCpf() == null || order.getBuyerCpf().isBlank()) {
            throw new BusinessException("CPF do comprador é obrigatório para gerar a etiqueta");
        }
        if (isBlank(order.getCep()) || isBlank(order.getRua()) || isBlank(order.getNumero())
                || isBlank(order.getBairro()) || isBlank(order.getCidade()) || isBlank(order.getEstado())) {
            throw new BusinessException("Pedido sem endereço completo para gerar a etiqueta");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BusinessException("Pedido sem itens para gerar a etiqueta");
        }
    }

    private ZonedDateTime parseExpiraEm(String expiraEm) {
        if (expiraEm == null || expiraEm.isBlank()) {
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

    private static String firstText(JsonNode node, String... fields) {
        if (node == null) return null;
        for (String f : fields) {
            JsonNode n = node.get(f);
            if (n != null && !n.isNull() && !n.asText().isBlank()) {
                return n.asText();
            }
        }
        return null;
    }

    private static String toDataUri(String base64) {
        String clean = stripDataUri(base64);
        return "data:application/pdf;base64," + clean;
    }

    private static String stripDataUri(String value) {
        if (value == null) return null;
        int idx = value.indexOf("base64,");
        if (idx >= 0) return value.substring(idx + 7).trim();
        return value.replaceAll("\\s", "");
    }

    private static String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String safe(String s) {
        return trim(s);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String truncate(String s) {
        return truncate(s, 300);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
