package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Teste simples de autenticação direta com a API dos Correios.
 *
 * Este teste faz uma chamada real à API dos Correios, então só roda quando
 * as credenciais estiverem disponíveis via variáveis de ambiente
 * (CORREIOS_USERNAME e CORREIOS_ACCESS_CODE). Caso contrário, é ignorado.
 */
class CorreiosServiceTest {

    private CorreiosService correiosService;

    @BeforeEach
    void setUp() {
        correiosService = new CorreiosService();

        String username = System.getenv("CORREIOS_USERNAME");
        String accessCode = System.getenv("CORREIOS_ACCESS_CODE");

        ReflectionTestUtils.setField(correiosService, "username", username);
        ReflectionTestUtils.setField(correiosService, "accessCode", accessCode);
    }

    @Test
    void deveObterTokenValidoDaApiDosCorreios() {
        String username = System.getenv("CORREIOS_USERNAME");
        String accessCode = System.getenv("CORREIOS_ACCESS_CODE");

        assumeTrue(username != null && !username.isBlank(), "CORREIOS_USERNAME não configurado, ignorando teste");
        assumeTrue(accessCode != null && !accessCode.isBlank(), "CORREIOS_ACCESS_CODE não configurado, ignorando teste");

        String token = correiosService.getAuthToken();

        assertNotNull(token, "O token retornado pelos Correios não deveria ser nulo");
        assertFalse(token.isBlank(), "O token retornado pelos Correios não deveria ser vazio");
    }
}
