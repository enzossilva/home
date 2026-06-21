package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.FreteService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/frete")
public class FreteController {
    private static final Logger logger = LoggerFactory.getLogger(FreteController.class);

    private final FreteService freteService;

    public FreteController(FreteService freteService) {
        this.freteService = freteService;
    }

    @GetMapping("/calcular")
    public ResponseEntity<?> calcular(
            @RequestParam @NotBlank(message = "CEP é obrigatório")
            @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido") String cep,
            @RequestParam(defaultValue = "0.3") double peso,
            @RequestParam(defaultValue = "5") double altura,
            @RequestParam(defaultValue = "20") double largura,
            @RequestParam(defaultValue = "30") double comprimento) {

        logger.info("Calculando frete: cep={}", cep);
        List<Map<String, Object>> resultado = freteService.calcularFrete(cep, peso, altura, largura, comprimento);
        return ResponseEntity.ok(ApiResponse.success(resultado));
    }
}
