package com.example.demo.controller;

import com.example.demo.model.LookbookItem;
import com.example.demo.repository.LookbookRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lookbook")
public class LookbookController {

    private final LookbookRepository repo;

    public LookbookController(LookbookRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<LookbookItem> getAll() {
        return repo.findAllByOrderByOrdemAscIdAsc();
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody LookbookItem item) {
        return ResponseEntity.ok(repo.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("mensagem", "Removido"));
    }
}
