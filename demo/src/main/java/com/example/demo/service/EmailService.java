package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Envio de e-mails.
 * Preferência: Resend (HTTP) — funciona no Railway.
 * Fallback: SMTP (Spring Mail) — útil em local / se MAIL_* estiver ok.
 */
@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.frontend.url:https://localhost:3000}")
    private String frontendUrl;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    /** Dispara o email em background para não travar o checkout. */
    public void enviarConfirmacaoPedidoAsync(Order order) {
        if (order == null || order.getUser() == null) return;
        final Long orderId = order.getId();
        final String to = order.getUser().getEmail();
        final String html = buildConfirmacaoHtml(order);
        final String subject = "Pedido #" + orderId + " confirmado — Young Zone";

        CompletableFuture.runAsync(() -> {
            try {
                send(to, subject, html);
                logger.info("Email de confirmação enviado: orderId={}, email={}", orderId, to);
            } catch (Exception e) {
                logger.error("Erro ao enviar email de confirmação para orderId={}", orderId, e);
            }
        });
    }

    public void enviarConfirmacaoPedido(Order order) {
        try {
            send(order.getUser().getEmail(),
                    "Pedido #" + order.getId() + " confirmado — Young Zone",
                    buildConfirmacaoHtml(order));
            logger.info("Email de confirmação enviado: orderId={}, email={}", order.getId(), order.getUser().getEmail());
        } catch (Exception e) {
            logger.error("Erro ao enviar email de confirmação para orderId={}", order.getId(), e);
        }
    }

    public void enviarResetSenha(com.example.demo.model.User user, String resetToken) {
        String base = frontendUrl == null ? "" : frontendUrl.replaceAll("/$", "");
        String resetUrl = base + "/reset-senha?token=" + resetToken;
        try {
            send(user.getEmail(), "Redefinição de senha — Young Zone", buildResetSenhaHtml(user, resetUrl));
            logger.info("Email de reset de senha enviado: email={} urlBase={}", user.getEmail(), base);
        } catch (Exception e) {
            logger.error("Erro ao enviar email de reset: email={}", user.getEmail(), e);
            throw new RuntimeException("Falha ao enviar email de reset", e);
        }
    }

    public void enviarCodigoRastreio(Order order) {
        try {
            send(order.getUser().getEmail(),
                    "Seu pedido #" + order.getId() + " foi enviado! — Young Zone",
                    buildRastreioHtml(order));
            logger.info("Email de rastreio enviado: orderId={}, tracking={}", order.getId(), order.getTrackingCode());
        } catch (Exception e) {
            logger.error("Erro ao enviar email de rastreio: orderId={}", order.getId(), e);
        }
    }

    private void send(String to, String subject, String html) throws Exception {
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            sendViaResend(to, subject, html);
            return;
        }
        sendViaSmtp(to, subject, html);
    }

    private void sendViaResend(String to, String subject, String html) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from);
        body.put("to", List.of(to));
        body.put("subject", subject);
        body.put("html", html);

        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Resend HTTP " + response.statusCode() + ": " + response.body());
        }
        logger.debug("Resend ok: {}", response.body());
    }

    private void sendViaSmtp(String to, String subject, String html) throws Exception {
        if (mailSender == null) {
            throw new IllegalStateException(
                    "E-mail não configurado. Defina RESEND_API_KEY (recomendado no Railway) ou MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD.");
        }
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setFrom(from);
        h.setTo(to);
        h.setSubject(subject);
        h.setText(html, true);
        mailSender.send(msg);
    }

    private String buildConfirmacaoHtml(Order order) {
        StringBuilder itens = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                itens.append("<tr>")
                     .append("<td style='padding:8px;border-bottom:1px solid #eee'>").append(escapeHtml(item.getProductName()));
                if (item.getSize() != null && !item.getSize().isBlank()) {
                    itens.append(" — Tam: ").append(escapeHtml(item.getSize()));
                }
                itens.append("</td>")
                     .append("<td style='padding:8px;border-bottom:1px solid #eee;text-align:right'>R$ ")
                     .append(String.format("%.2f", item.getProductPrice())).append("</td>")
                     .append("</tr>");
            }
        }

        String servico = "SEDEX".equals(order.getShippingMethod()) ? "SEDEX" : "PAC";

        StringBuilder address = new StringBuilder();
        address.append(escapeHtml(order.getRua())).append(", ").append(escapeHtml(order.getNumero()));
        if (order.getComplemento() != null && !order.getComplemento().isBlank()) {
            address.append(", ").append(escapeHtml(order.getComplemento()));
        }
        address.append("<br>")
               .append(escapeHtml(order.getBairro())).append(" — ")
               .append(escapeHtml(order.getCidade())).append("/").append(escapeHtml(order.getEstado()))
               .append("<br>CEP: ").append(escapeHtml(order.getCep()));

        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto'>"
            + "<div style='background:#111;padding:24px;text-align:center'>"
            + "<h1 style='color:#fff;margin:0;font-size:22px'>Young Zone</h1></div>"
            + "<div style='padding:32px'>"
            + "<h2>Pedido #" + order.getId() + " recebido!</h2>"
            + "<p>Olá, <strong>" + escapeHtml(order.getUser().getName()) + "</strong>! Recebemos seu pedido.</p>"
            + "<p style='color:#666;font-size:14px'>O pedido fica confirmado após a aprovação do pagamento.</p>"
            + "<table style='width:100%;border-collapse:collapse;margin:20px 0'>"
            + "<thead><tr style='background:#f5f5f5'>"
            + "<th style='padding:8px;text-align:left'>Produto</th>"
            + "<th style='padding:8px;text-align:right'>Valor</th>"
            + "</tr></thead><tbody>" + itens + "</tbody></table>"
            + "<table style='width:100%'>"
            + "<tr><td>Subtotal</td><td style='text-align:right'>R$ " + String.format("%.2f", nz(order.getSubtotal())) + "</td></tr>"
            + "<tr><td>Frete (" + servico + ")</td><td style='text-align:right'>R$ " + String.format("%.2f", nz(order.getShippingCost())) + "</td></tr>"
            + "<tr><td><strong>Total</strong></td><td style='text-align:right'><strong>R$ " + String.format("%.2f", nz(order.getTotal())) + "</strong></td></tr>"
            + "</table>"
            + "<hr style='margin:24px 0'>"
            + "<h3>Endereço de entrega</h3>"
            + "<p>" + address + "</p>"
            + "<p style='color:#666;font-size:13px'>Assim que o pedido for enviado, você recebe o código de rastreio por e-mail.</p>"
            + "</div></div>";
    }

    private String buildResetSenhaHtml(com.example.demo.model.User user, String resetUrl) {
        return "<div style='font-family:Arial,sans-serif;max-width:520px;margin:0 auto'>"
            + "<div style='background:#111;padding:24px;text-align:center'>"
            + "<h1 style='color:#fff;margin:0;font-size:22px'>Young Zone</h1></div>"
            + "<div style='padding:32px'>"
            + "<h2>Redefinir senha</h2>"
            + "<p>Olá, <strong>" + escapeHtml(user.getName()) + "</strong>!</p>"
            + "<p>Recebemos um pedido de redefinição de senha. Clique no botão abaixo:</p>"
            + "<div style='text-align:center;margin:32px 0'>"
            + "<a href='" + escapeHtml(resetUrl) + "' style='background:#111;color:#fff;padding:14px 32px;text-decoration:none;font-weight:bold;display:inline-block;border-radius:4px'>REDEFINIR SENHA</a>"
            + "</div>"
            + "<p style='color:#888;font-size:13px'>Este link expira em <strong>1 hora</strong>. Se você não solicitou, ignore este e-mail.</p>"
            + "</div></div>";
    }

    private String buildRastreioHtml(Order order) {
        String servico = "SEDEX".equals(order.getShippingMethod()) ? "SEDEX" : "PAC";
        String rastreioUrl = "https://rastreamento.correios.com.br/app/index.php?objeto=" + order.getTrackingCode();

        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto'>"
            + "<div style='background:#111;padding:24px;text-align:center'>"
            + "<h1 style='color:#fff;margin:0;font-size:22px'>Young Zone</h1></div>"
            + "<div style='padding:32px'>"
            + "<h2>Seu pedido foi enviado!</h2>"
            + "<p>Olá, <strong>" + escapeHtml(order.getUser().getName()) + "</strong>! Seu pedido #" + order.getId() + " foi postado nos Correios.</p>"
            + "<div style='background:#f5f5f5;border-radius:8px;padding:20px;margin:20px 0;text-align:center'>"
            + "<p style='margin:0 0 8px;color:#666;font-size:14px'>Código de rastreio (" + servico + ")</p>"
            + "<h2 style='margin:0;font-size:28px;letter-spacing:4px'>" + escapeHtml(order.getTrackingCode()) + "</h2>"
            + "</div>"
            + "<div style='text-align:center;margin:24px 0'>"
            + "<a href='" + rastreioUrl + "' style='background:#111;color:#fff;padding:14px 32px;text-decoration:none;font-weight:bold;display:inline-block'>RASTREAR ENCOMENDA</a>"
            + "</div>"
            + "<p style='color:#666;font-size:13px'>Ou acesse <a href='https://correios.com.br'>correios.com.br</a> e insira o código: <strong>" + escapeHtml(order.getTrackingCode()) + "</strong></p>"
            + "</div></div>";
    }

    private double nz(Double v) {
        return v != null ? v : 0.0;
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
