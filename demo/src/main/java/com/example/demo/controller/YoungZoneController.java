package com.example.demo.controller;

import com.example.demo.config.AuthHelper;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller de produtos (anteriormente YoungZoneController).
 * GET /products - Público
 * POST/PUT/DELETE /products - Admin
 */
@RestController
@RequestMapping("/products")
public class YoungZoneController {
    private static final Logger logger = LoggerFactory.getLogger(YoungZoneController.class);

    private final ProductService productService;
    private final AuthHelper authHelper;

    public YoungZoneController(ProductService productService, AuthHelper authHelper) {
        this.productService = productService;
        this.authHelper = authHelper;
    }

    @GetMapping
    public ResponseEntity<?> getProducts() {
        logger.info("Listando produtos");
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping
    public ResponseEntity<?> addProduct(@RequestBody Product product, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Criando novo produto: {}", product.getName());
        Product created = productService.addProduct(product);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Produto criado com sucesso"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Deletando produto: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Produto deletado com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product product, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Atualizando produto: {}", id);
        Product updated = productService.updateProduct(id, product);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(updated, "Produto atualizado com sucesso"));
    }
}
