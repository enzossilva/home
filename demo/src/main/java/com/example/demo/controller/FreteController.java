package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.OrderService;
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
            @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido") String cep) {

        logger.info("Calculando frete: cep={}", cep);

        double pac = OrderService.calcularFrete(cep, "PAC");
        double sedex = OrderService.calcularFrete(cep, "SEDEX");
        String[] prazos = OrderService.prazosFrete(cep);

        List<Map<String, Object>> resultado = new ArrayList<>();

        Map<String, Object> pacOpt = new HashMap<>();
        pacOpt.put("service", "PAC");
        pacOpt.put("name", "PAC — Correios");
        pacOpt.put("price", pac);
        pacOpt.put("days", prazos[0]);
        resultado.add(pacOpt);

        Map<String, Object> sedexOpt = new HashMap<>();
        sedexOpt.put("service", "SEDEX");
        sedexOpt.put("name", "SEDEX — Correios");
        sedexOpt.put("price", sedex);
        sedexOpt.put("days", prazos[1]);
        resultado.add(sedexOpt);

        return ResponseEntity.ok(ApiResponse.success(resultado));
    }
}
