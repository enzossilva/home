package com.example.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthHelper {

    private final JwtUtil jwtUtil;

    public AuthHelper(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public Long getUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return jwtUtil.extractUserId(header.substring(7));
        }
        throw new SecurityException("Token não encontrado");
    }

    public boolean isAdmin(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return "ADMIN".equals(jwtUtil.extractRole(header.substring(7)));
        }
        return false;
    }

    public void requireOwnerOrAdmin(HttpServletRequest request, Long resourceOwnerId) {
        Long tokenUserId = getUserId(request);
        if (!tokenUserId.equals(resourceOwnerId) && !isAdmin(request)) {
            throw new SecurityException("Acesso negado");
        }
    }
}
