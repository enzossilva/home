package com.example.demo.controller;

import com.example.demo.config.AuthHelper;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.Cart;
import com.example.demo.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final AuthHelper authHelper;

    public CartController(CartService cartService, AuthHelper authHelper) {
        this.cartService = cartService;
        this.authHelper = authHelper;
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = authHelper.getUserId(request);
        Long productId = Long.valueOf(body.get("productId").toString());
        Integer quantity = Integer.valueOf(body.get("quantity").toString());
        String size = body.get("size") != null ? body.get("size").toString() : null;

        logger.info("Adicionando produto ao carrinho: userId={}, productId={}, quantity={}", userId, productId, quantity);
        Cart cart = cartService.addToCart(userId, productId, quantity, size);
        return ResponseEntity.ok(ApiResponse.success(cart, "Produto adicionado ao carrinho"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId, HttpServletRequest request) {
        authHelper.requireOwnerOrAdmin(request, userId);
        logger.info("Recuperando carrinho: userId={}", userId);
        List<Cart> items = cartService.getCartByUser(userId);
        Double total = cartService.getCartTotal(userId);
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("items", items, "total", total)
        ));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long cartItemId, HttpServletRequest request) {
        Long tokenUserId = authHelper.getUserId(request);
        boolean isAdmin = authHelper.isAdmin(request);

        logger.info("Removendo item do carrinho: cartItemId={}, userId={}", cartItemId, tokenUserId);
        cartService.removeFromCartSecure(cartItemId, tokenUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(null, "Item removido do carrinho"));
    }
}
