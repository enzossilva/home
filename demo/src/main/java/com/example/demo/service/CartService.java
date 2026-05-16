package com.example.demo.service;

import com.example.demo.model.Cart;
import com.example.demo.model.Product;
import com.example.demo.model.ProductSize;
import com.example.demo.model.User;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSizeRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       ProductSizeRepository productSizeRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.productSizeRepository = productSizeRepository;
        this.userRepository = userRepository;
    }

    public Cart addToCart(Long userId, Long productId, Integer quantity, String size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        // Verifica estoque por tamanho se o produto tem tamanhos cadastrados
        if (size != null && !size.isBlank()) {
            ProductSize ps = productSizeRepository.findByProductAndSize(product, size)
                    .orElseThrow(() -> new RuntimeException("Tamanho " + size + " não disponível"));
            if (ps.getStock() < quantity) {
                throw new RuntimeException("Estoque insuficiente para o tamanho " + size);
            }
        } else {
            if (product.getStock() < quantity) {
                throw new RuntimeException("Estoque insuficiente");
            }
        }

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setProduct(product);
        cart.setQuantity(quantity);
        cart.setSize(size);
        return cartRepository.save(cart);
    }

    public List<Cart> getCartByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return cartRepository.findByUser(user);
    }

    public Double getCartTotal(Long userId) {
        return getCartByUser(userId).stream()
                .mapToDouble(Cart::getSubtotal)
                .sum();
    }

    public void removeFromCart(Long cartItemId) {
        cartRepository.deleteById(cartItemId);
    }

    public void clearCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        cartRepository.deleteByUser(user);
    }
}
