package com.example.demo.controller;

import com.example.demo.config.JwtUtil;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User created = userService.createUser(user);
            String token = jwtUtil.generateToken(created.getEmail(), created.getRole(), created.getId());
            created.setPassword(null);
            return ResponseEntity.ok(Map.of("user", created, "token", token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            User user = userService.login(body.get("email"), body.get("password"));
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());
            user.setPassword(null);
            return ResponseEntity.ok(Map.of("user", user, "token", token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            User updated = userService.updateProfile(id, body.get("name"), body.get("email"), body.get("password"));
            String token = jwtUtil.generateToken(updated.getEmail(), updated.getRole(), updated.getId());
            updated.setPassword(null);
            return ResponseEntity.ok(Map.of("user", updated, "token", token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/reset-request")
    public ResponseEntity<?> resetRequest(@RequestBody Map<String, String> body,
                                          jakarta.servlet.http.HttpServletRequest request) {
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                       ? ":" + request.getServerPort() : "");
            userService.requestPasswordReset(body.get("email"), baseUrl);
            return ResponseEntity.ok(Map.of("mensagem", "Email enviado com o link de redefinição"));
        } catch (Exception e) {
            // Responde igual mesmo se email não existir (segurança)
            return ResponseEntity.ok(Map.of("mensagem", "Se o email estiver cadastrado, você receberá o link em breve"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            userService.resetPassword(body.get("token"), body.get("password"));
            return ResponseEntity.ok(Map.of("mensagem", "Senha redefinida com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listUsers() {
        try {
            return ResponseEntity.ok(userService.getUsers());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String role = body.get("role");
            if (role == null || role.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Role inválida"));
            }
            User updated = userService.updateRole(id, role);
            updated.setPassword(null);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}
