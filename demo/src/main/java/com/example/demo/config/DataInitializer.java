package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Cria o admin inicial somente se ADMIN_EMAIL e ADMIN_PASSWORD estiverem
 * definidos no ambiente (Railway Variables). Sem senha no código.
 */
@Configuration
public class DataInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.name:Administrador}")
    private String adminName;

    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            if (adminEmail == null || adminEmail.isBlank()
                    || adminPassword == null || adminPassword.isBlank()) {
                logger.info("ADMIN_EMAIL/ADMIN_PASSWORD não definidos — seed de admin ignorado");
                return;
            }
            String email = adminEmail.trim().toLowerCase();
            if (userRepository.existsByEmail(email)) {
                logger.info("Admin já existe: {}", email);
                return;
            }
            User admin = new User();
            admin.setName(adminName != null && !adminName.isBlank() ? adminName.trim() : "Administrador");
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            logger.info("Admin criado a partir das variáveis de ambiente: {}", email);
        };
    }
}
