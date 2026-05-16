package com.example.demo.controller;

import com.example.demo.service.FreteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/frete")
public class FreteController {

    private final FreteService freteService;

    public FreteController(FreteService freteService) {
        this.freteService = freteService;
    }

    @GetMapping("/calcular")
    public ResponseEntity<?> calcular(
            @RequestParam String cep,
            @RequestParam(defaultValue = "0.3") double peso,
            @RequestParam(defaultValue = "5") double altura,
            @RequestParam(defaultValue = "20") double largura,
            @RequestParam(defaultValue = "30") double comprimento) {
        try {
            return ResponseEntity.ok(freteService.calcularFrete(cep, peso, altura, largura, comprimento));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}
