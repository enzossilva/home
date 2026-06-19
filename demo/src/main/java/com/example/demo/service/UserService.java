package com.example.demo.service;

import com.example.demo.dto.UserResponse;
import com.example.demo.enums.UserRole;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int RESET_TOKEN_EXPIRY_HOURS = 1;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public UserResponse createUser(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            logger.warn("Tentativa de registro com email já existente: {}", email);
            throw new BusinessException("Email já cadastrado");
        }

        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new ValidationException("Senha deve ter no mínimo " + PASSWORD_MIN_LENGTH + " caracteres");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(UserRole.CUSTOMER.getCode());

        User saved = userRepository.save(user);
        logger.info("Novo usuário criado: id={}, email={}", saved.getId(), email);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Email ou senha incorretos"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.warn("Login falhou para email: {} - senha incorreta", email);
            throw new BusinessException("Email ou senha incorretos");
        }

        logger.info("Login bem-sucedido: id={}, email={}", user.getId(), email);
        return toResponse(user);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", null));

        String token = SecurityUtils.generateSecureToken();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS));
        userRepository.save(user);

        logger.info("Token de reset gerado para: {}", email);
        emailService.enviarResetSenha(user, token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword.length() < PASSWORD_MIN_LENGTH) {
            throw new ValidationException("Senha deve ter no mínimo " + PASSWORD_MIN_LENGTH + " caracteres");
        }

        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new BusinessException("Token inválido ou expirado"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            logger.warn("Tentativa de reset com token expirado");
            throw new BusinessException("Token expirado. Solicite um novo link.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        logger.info("Senha resetada para: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateRole(Long id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        try {
            UserRole userRole = UserRole.fromCode(role);
            user.setRole(userRole.getCode());
            User updated = userRepository.save(user);
            logger.info("Papel atualizado para usuário id={}: {}", id, role);
            return toResponse(updated);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Papel inválido: " + role);
        }
    }

    @Transactional
    public UserResponse updateProfile(Long id, String name, String email, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        if (name != null && !name.isBlank()) {
            user.setName(name);
        }

        if (email != null && !email.isBlank() && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException("Email já cadastrado por outro usuário");
            }
            user.setEmail(email);
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < PASSWORD_MIN_LENGTH) {
                throw new ValidationException("Senha deve ter no mínimo " + PASSWORD_MIN_LENGTH + " caracteres");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        User updated = userRepository.save(user);
        logger.info("Perfil atualizado para usuário id={}", id);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
