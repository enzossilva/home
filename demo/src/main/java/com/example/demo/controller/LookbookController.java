package com.example.demo.controller;

import com.example.demo.config.AuthHelper;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.LookbookItem;
import com.example.demo.repository.LookbookRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lookbook")
public class LookbookController {
    private static final Logger logger = LoggerFactory.getLogger(LookbookController.class);

    private final LookbookRepository repo;
    private final AuthHelper authHelper;

    public LookbookController(LookbookRepository repo, AuthHelper authHelper) {
        this.repo = repo;
        this.authHelper = authHelper;
    }

    /**
     * GET /lookbook - Público
     */
    @GetMapping
    public ResponseEntity<?> getAll() {
        logger.info("Listing lookbook items");
        List<LookbookItem> items = repo.findAllByOrderByOrdemAscIdAsc();
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * POST /lookbook - Admin only
     */
    @PostMapping
    public ResponseEntity<?> add(@Valid @RequestBody LookbookItem item, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Adding lookbook item: {}", item.getTitle());
        LookbookItem saved = repo.save(item);
        return ResponseEntity.status(201).body(ApiResponse.success(saved, "Item adicionado com sucesso"));
    }

    /**
     * DELETE /lookbook/{id} - Admin only
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Deleting lookbook item: {}", id);

        LookbookItem item = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado"));

        repo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Item removido com sucesso"));
    }
}
