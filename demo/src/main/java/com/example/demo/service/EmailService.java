package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarConfirmacaoPedido(Order order) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from);
            h.setTo(order.getUser().getEmail());
            h.setSubject("Pedido #" + order.getId() + " confirmado — Young Zone");
            h.setText(buildConfirmacaoHtml(order), true);
            mailSender.send(msg);
            logger.info("Email de confirmação enviado: orderId={}, email={}", order.getId(), order.getUser().getEmail());
        } catch (Exception e) {
            logger.error("Erro ao enviar email de confirmação para orderId={}", order.getId(), e);
        }
    }

    public void enviarResetSenha(com.example.demo.model.User user, String resetToken) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from);
            h.setTo(user.getEmail());
            h.setSubject("Redefinição de senha — Young Zone");
            h.setText(buildResetSenhaHtml(user, resetToken), true);
            mailSender.send(msg);
            logger.info("Email de reset de senha enviado: email={}", user.getEmail());
        } catch (Exception e) {
            logger.error("Erro ao enviar email de reset: email={}", user.getEmail(), e);
        }
    }

    public void enviarCodigoRastreio(Order order) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from);
            h.setTo(order.getUser().getEmail());
            h.setSubject("Seu pedido #" + order.getId() + " foi enviado! — Young Zone");
            h.setText(buildRastreioHtml(order), true);
            mailSender.send(msg);
            logger.info("Email de rastreio enviado: orderId={}, tracking={}", order.getId(), order.getTrackingCode());
        } catch (Exception e) {
            logger.error("Erro ao enviar email de rastreio: orderId={}", order.getId(), e);
        }
    }

    private String buildConfirmacaoHtml(Order order) {
        StringBuilder itens = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                itens.append("<tr>")
                     .append("<td style='padding:8px;border-bottom:1px solid #eee'>").append(item.getProductName());
                if (item.getSize() != null && !item.getSize().isBlank()) {
                    itens.append(" — Tam: ").append(item.getSize());
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
            + "<h2>Pedido #" + order.getId() + " confirmado!</h2>"
            + "<p>Olá, <strong>" + escapeHtml(order.getUser().getName()) + "</strong>! Recebemos seu pedido e ele está sendo processado.</p>"
            + "<table style='width:100%;border-collapse:collapse;margin:20px 0'>"
            + "<thead><tr style='background:#f5f5f5'>"
            + "<th style='padding:8px;text-align:left'>Produto</th>"
            + "<th style='padding:8px;text-align:right'>Valor</th>"
            + "</tr></thead><tbody>" + itens + "</tbody></table>"
            + "<table style='width:100%'>"
            + "<tr><td>Subtotal</td><td style='text-align:right'>R$ " + String.format("%.2f", order.getSubtotal()) + "</td></tr>"
            + "<tr><td>Frete (" + servico + ")</td><td style='text-align:right'>R$ " + String.format("%.2f", order.getShippingCost()) + "</td></tr>"
            + "<tr><td><strong>Total</strong></td><td style='text-align:right'><strong>R$ " + String.format("%.2f", order.getTotal()) + "</strong></td></tr>"
            + "</table>"
            + "<hr style='margin:24px 0'>"
            + "<h3>Endereço de entrega</h3>"
            + "<p>" + address + "</p>"
            + "<p style='color:#666;font-size:13px'>Assim que seu pedido for enviado pelos Correios, você receberá um email com o código de rastreio.</p>"
            + "</div></div>";
    }

    private String buildResetSenhaHtml(com.example.demo.model.User user, String resetToken) {
        return "<div style='font-family:Arial,sans-serif;max-width:520px;margin:0 auto'>"
            + "<div style='background:#111;padding:24px;text-align:center'>"
            + "<h1 style='color:#fff;margin:0;font-size:22px'>Young Zone</h1></div>"
            + "<div style='padding:32px'>"
            + "<h2>Redefinir senha</h2>"
            + "<p>Olá, <strong>" + escapeHtml(user.getName()) + "</strong>!</p>"
            + "<p>Recebemos um pedido de redefinição de senha para sua conta. Clique no botão abaixo:</p>"
            + "<div style='text-align:center;margin:32px 0'>"
            + "<a href='" + resetToken + "' style='background:#111;color:#fff;padding:14px 32px;text-decoration:none;font-weight:bold;display:inline-block;border-radius:4px'>REDEFINIR SENHA</a>"
            + "</div>"
            + "<p style='color:#888;font-size:13px'>Este link expira em <strong>1 hora</strong>. Se você não solicitou isso, ignore este email.</p>"
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
            + "<h2 style='margin:0;font-size:28px;letter-spacing:4px'>" + order.getTrackingCode() + "</h2>"
            + "</div>"
            + "<div style='text-align:center;margin:24px 0'>"
            + "<a href='" + rastreioUrl + "' style='background:#111;color:#fff;padding:14px 32px;text-decoration:none;font-weight:bold;display:inline-block'>RASTREAR ENCOMENDA</a>"
            + "</div>"
            + "<p style='color:#666;font-size:13px'>Ou acesse <a href='https://correios.com.br'>correios.com.br</a> e insira o código: <strong>" + order.getTrackingCode() + "</strong></p>"
            + "</div></div>";
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
