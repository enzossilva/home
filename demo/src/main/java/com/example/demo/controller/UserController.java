package com.example.demo.controller;

import com.example.demo.config.AuthHelper;
import com.example.demo.dto.*;
import com.example.demo.config.JwtUtil;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthHelper authHelper;

    public UserController(UserService userService, JwtUtil jwtUtil, AuthHelper authHelper) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authHelper = authHelper;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegisterRequest request) {
        logger.info("Registro de novo usuário: {}", request.getEmail());
        UserResponse userResponse = userService.createUser(request.getName(), request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken(userResponse.getEmail(), userResponse.getRole(), userResponse.getId());
        AuthResponse response = new AuthResponse(userResponse, token);
        return ResponseEntity.ok(ApiResponse.success(response, "Usuário registrado com sucesso"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequest request) {
        logger.info("Login: {}", request.getEmail());
        UserResponse userResponse = userService.login(request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken(userResponse.getEmail(), userResponse.getRole(), userResponse.getId());
        AuthResponse response = new AuthResponse(userResponse, token);
        return ResponseEntity.ok(ApiResponse.success(response, "Login realizado com sucesso"));
    }

    @PostMapping("/reset-request")
    public ResponseEntity<?> resetRequest(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        logger.info("Solicitação de reset de senha: {}", email);
        userService.requestPasswordReset(email);
        // Responde igual mesmo se email não existir (segurança - user enumeration prevention)
        return ResponseEntity.ok(ApiResponse.success(null, "Se o email estiver cadastrado, você receberá um link em breve"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        logger.info("Reset de senha com token");
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Senha redefinida com sucesso"));
    }

    @GetMapping
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
        authHelper.requireAdmin(request);
        logger.info("Listando todos os usuários");
        List<UserResponse> users = userService.getUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        String role = body.get("role");
        logger.info("Atualizando papel do usuário id={} para {}", id, role);
        UserResponse updated = userService.updateRole(id, role);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        authHelper.requireOwnerOrAdmin(request, id);
        logger.info("Atualizando perfil do usuário id={}", id);
        UserResponse updated = userService.updateProfile(id, body.get("name"), body.get("email"), body.get("password"));
        String token = jwtUtil.generateToken(updated.getEmail(), updated.getRole(), updated.getId());
        AuthResponse response = new AuthResponse(updated, token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

class ResetPasswordRequest {
    private String token;
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
