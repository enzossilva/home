package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.CorreiosService;
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

    private final CorreiosService correiosService;

    public FreteController(CorreiosService correiosService) {
        this.correiosService = correiosService;
    }

    @GetMapping("/calcular")
    public ResponseEntity<?> calcular(
            @RequestParam @NotBlank(message = "CEP é obrigatório")
            @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido") String cep) {

        logger.info("Calculando frete: cep={}", cep);
        List<Map<String, Object>> resultado = correiosService.cotarOpcoesFrete(cep);
        return ResponseEntity.ok(ApiResponse.success(resultado));
    }
}
