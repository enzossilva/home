package com.example.demo.controller;

import com.example.demo.config.AuthHelper;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.HeroConfig;
import com.example.demo.repository.HeroConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hero")
public class HeroController {

    private final HeroConfigRepository repo;
    private final AuthHelper authHelper;

    public HeroController(HeroConfigRepository repo, AuthHelper authHelper) {
        this.repo = repo;
        this.authHelper = authHelper;
    }

    @GetMapping
    public ResponseEntity<?> get() {
        return repo.findById(1L)
                .map(h -> ResponseEntity.ok(ApiResponse.success(h)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    @PostMapping
    public ResponseEntity<?> set(@RequestBody HeroConfig hero, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        hero.setId(1L);
        HeroConfig saved = repo.save(hero);
        return ResponseEntity.ok(ApiResponse.success(saved, "Hero atualizado"));
    }
}
