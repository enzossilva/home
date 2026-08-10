package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Teste de autenticação com cartão de postagem (só roda com credenciais no ambiente).
 */
class CorreiosServiceTest {

    private CorreiosService correiosService;

    @BeforeEach
    void setUp() {
        correiosService = new CorreiosService();

        ReflectionTestUtils.setField(correiosService, "username", System.getenv("CORREIOS_USERNAME"));
        ReflectionTestUtils.setField(correiosService, "accessCode", System.getenv("CORREIOS_ACCESS_CODE"));
        ReflectionTestUtils.setField(correiosService, "cartaoPostagem", System.getenv("CORREIOS_CARTAO_POSTAGEM"));
        ReflectionTestUtils.setField(correiosService, "contrato", System.getenv("CORREIOS_CONTRATO"));
        ReflectionTestUtils.setField(correiosService, "dr", System.getenv("CORREIOS_DR"));
        ReflectionTestUtils.setField(correiosService, "ambiente",
                System.getenv().getOrDefault("CORREIOS_AMBIENTE", "hom"));
    }

    @Test
    void deveObterTokenValidoComCartaoDePostagem() {
        assumeTrue(notBlank(System.getenv("CORREIOS_USERNAME")), "CORREIOS_USERNAME não configurado");
        assumeTrue(notBlank(System.getenv("CORREIOS_ACCESS_CODE")), "CORREIOS_ACCESS_CODE não configurado");
        assumeTrue(notBlank(System.getenv("CORREIOS_CARTAO_POSTAGEM")), "CORREIOS_CARTAO_POSTAGEM não configurado");

        String token = correiosService.getAuthToken();

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
