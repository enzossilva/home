package com.example.demo.repository;

import com.example.demo.model.Cart;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // Busca apenas o carrinho do usuário específico
    List<Cart> findByUser(User user);

    // Busca item específico no carrinho do usuário
    Optional<Cart> findByUserAndProductId(User user, Long productId);
    Optional<Cart> findByUserAndProductIdAndSize(User user, Long productId, String size);

    // Remove todos os itens do carrinho de um usuário (após pagamento)
    void deleteByUser(User user);

    // Remove todos os itens do carrinho que referenciam um produto
    void deleteByProductId(Long productId);
}
