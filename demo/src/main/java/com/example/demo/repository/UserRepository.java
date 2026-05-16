package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Busca usuário por email (usado no login)
    Optional<User> findByEmail(String email);

    // Verifica se email já existe (evita duplicatas)
    boolean existsByEmail(String email);

    Optional<User> findByResetToken(String resetToken);
}
