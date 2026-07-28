package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/frete")
public class FreteController {
    private static final Logger logger = LoggerFactory.getLogger(FreteController.class);

    @GetMapping("/calcular")
    public ResponseEntity<?> calcular(
            @RequestParam @NotBlank(message = "CEP é obrigatório")
            @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido") String cep,
            @RequestParam(defaultValue = "0.3") double peso,
            @RequestParam(defaultValue = "5") double altura,
            @RequestParam(defaultValue = "20") double largura,
            @RequestParam(defaultValue = "30") double comprimento) {

        logger.info("Calculando frete: cep={}", cep);

        // Tabela local rápida — evita chamar Correios e travar o checkout.
        List<Map<String, Object>> resultado = freteFallback(cep);
        return ResponseEntity.ok(ApiResponse.success(resultado));
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
