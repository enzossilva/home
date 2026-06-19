package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.SecurityUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Inicializa dados padrão da aplicação (admin user).
 * IMPORTANTE: Em produção, o admin deve ser criado via migração ou script seguro.
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail("admin@youngzone.com")) {
                User admin = new User();
                admin.setName("Administrador");
                admin.setEmail("admin@youngzone.com");
                // Gera senha forte aleatória em produção - isto é APENAS para dev
                String defaultPassword = SecurityUtils.generateSecureToken(12);
                admin.setPassword(passwordEncoder.encode(defaultPassword));
                admin.setRole("ADMIN");
                userRepository.save(admin);
                System.out.println("⚠️  ADMIN CRIADO (DEV ONLY)");
                System.out.println("   Email: admin@youngzone.com");
                System.out.println("   ⚠️  ALTERE A SENHA IMEDIATAMENTE EM PRODUÇÃO");
            }
        };
    }
}
