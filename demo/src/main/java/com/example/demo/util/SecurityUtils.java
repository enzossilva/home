package com.example.demo.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Utilidades para operações cryptográficas e de segurança.
 */
public class SecurityUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Gera um token seguro criptograficamente para reset de senha.
     * USA SecureRandom ao invés de UUID.randomUUID() para maior segurança.
     */
    public static String generateSecureToken(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder token = new StringBuilder();
        for (byte b : bytes) {
            token.append(String.format("%02x", b));
        }
        return token.toString();
    }

    /**
     * Gera um token de 32 bytes (256 bits) - suficientemente seguro
     */
    public static String generateSecureToken() {
        return generateSecureToken(32);
    }

    /**
     * Válida um CPF (mínimo preenchimento)
     */
    public static boolean isValidCPF(String cpf) {
        if (cpf == null) return false;
        String clean = cpf.replaceAll("[^0-9]", "");
        return clean.length() == 11 && !clean.matches("^(\\d)\\1{10}$");
    }
}

