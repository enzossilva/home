package com.example.demo.controller;

import com.example.demo.config.AuthHelper;
import com.example.demo.model.Cart;
import com.example.demo.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final AuthHelper authHelper;

    public CartController(CartService cartService, AuthHelper authHelper) {
        this.cartService = cartService;
        this.authHelper = authHelper;
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        try {
            // userId vem do token JWT — ignora o body para prevenir IDOR
            Long userId = authHelper.getUserId(request);
            Long productId = Long.valueOf(body.get("productId").toString());
            Integer quantity = Integer.valueOf(body.get("quantity").toString());
            String size = body.get("size") != null ? body.get("size").toString() : null;

            Cart cart = cartService.addToCart(userId, productId, quantity, size);
            return ResponseEntity.ok(cart);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId,
                                     HttpServletRequest request) {
        try {
            authHelper.requireOwnerOrAdmin(request, userId);
            List<Cart> items = cartService.getCartByUser(userId);
            Double total = cartService.getCartTotal(userId);
            return ResponseEntity.ok(Map.of("items", items, "total", total));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long cartItemId,
                                            HttpServletRequest request) {
        try {
            // Verifica que o item pertence ao usuário do token
            Long tokenUserId = authHelper.getUserId(request);
            cartService.removeFromCartSecure(cartItemId, tokenUserId, authHelper.isAdmin(request));
            return ResponseEntity.ok(Map.of("mensagem", "Item removido"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}
