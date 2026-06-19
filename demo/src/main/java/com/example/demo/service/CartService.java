package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Cart;
import com.example.demo.model.Product;
import com.example.demo.model.ProductSize;
import com.example.demo.model.User;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSizeRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Cart addToCart(Long userId, Long productId, Integer quantity, String size) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("Quantidade deve ser maior que zero");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));

        // Verifica estoque por tamanho se o produto tem tamanhos cadastrados
        if (size != null && !size.isBlank()) {
            ProductSize ps = productSizeRepository.findByProductAndSize(product, size)
                    .orElseThrow(() -> new BusinessException("Tamanho " + size + " não disponível"));
            if (ps.getStock() == null || ps.getStock() < quantity) {
                throw new BusinessException("Estoque insuficiente para o tamanho " + size);
            }
        } else {
            if (product.getStock() == null || product.getStock() < quantity) {
                throw new BusinessException("Estoque insuficiente");
            }
        }

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setProduct(product);
        cart.setQuantity(quantity);
        cart.setSize(size);
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public List<Cart> getCartByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        return cartRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public Double getCartTotal(Long userId) {
        return getCartByUser(userId).stream()
                .mapToDouble(Cart::getSubtotal)
                .sum();
    }

    @Transactional
    public void removeFromCart(Long cartItemId) {
        Cart item = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item do carrinho", cartItemId));
        cartRepository.deleteById(cartItemId);
    }

    @Transactional
    public void removeFromCartSecure(Long cartItemId, Long tokenUserId, boolean isAdmin) {
        Cart item = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item do carrinho", cartItemId));

        if (!isAdmin && !item.getUser().getId().equals(tokenUserId)) {
            throw new ForbiddenException("Você não pode remover itens do carrinho de outro usuário");
        }

        cartRepository.deleteById(cartItemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        cartRepository.deleteByUser(user);
    }
}
