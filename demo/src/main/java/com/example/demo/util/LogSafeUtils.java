package com.example.demo.util;

/**
 * Utilitários para masking de dados sensíveis em logs.
 * Evita exposição de CPF, email, cartão em logs.
 */
public class LogSafeUtils {

    /**
     * Mascara CPF deixando apenas últimos 2 dígitos
     * "12345678901" → "123456789**"
     */
    public static String maskCPF(String cpf) {
        if (cpf == null || cpf.length() < 3) {
            return "***";
        }
        String clean = cpf.replaceAll("[^0-9]", "");
        if (clean.length() < 3) return "***";
        return clean.substring(0, clean.length() - 2) + "**";
    }

    /**
     * Mascara email deixando domínio apenas
     * "usuario@example.com" → "u****@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.***";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 1) {
            return "*@" + domain;
        }

        return local.charAt(0) + "****@" + domain;
    }

    /**
     * Mascara cartão deixando apenas últimos 4 dígitos
     * "1234567812345678" → "****5678"
     */
    public static String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String clean = cardNumber.replaceAll("[^0-9]", "");
        if (clean.length() < 4) return "****";
        return "****" + clean.substring(clean.length() - 4);
    }

    /**
     * Mascara token JWT deixando apenas primeiros 10 caracteres
     */
    public static String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 10) + "...";
    }
}

