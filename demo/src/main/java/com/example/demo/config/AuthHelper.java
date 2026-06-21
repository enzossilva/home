package com.example.demo.config;

import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.UnauthorizedException;
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
        throw new UnauthorizedException("Token não encontrado");
    }

    public String getUserEmail(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return jwtUtil.extractEmail(header.substring(7));
        }
        throw new UnauthorizedException("Token não encontrado");
    }

    public String getUserRole(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return jwtUtil.extractRole(header.substring(7));
        }
        throw new UnauthorizedException("Token não encontrado");
    }

    public boolean isAdmin(HttpServletRequest request) {
        try {
            String role = getUserRole(request);
            return "ADMIN".equals(role);
        } catch (Exception e) {
            return false;
        }
    }

    public void requireOwnerOrAdmin(HttpServletRequest request, Long resourceOwnerId) {
        Long tokenUserId = getUserId(request);
        if (!tokenUserId.equals(resourceOwnerId) && !isAdmin(request)) {
            throw new ForbiddenException("Acesso negado");
        }
    }

    public void requireAdmin(HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new ForbiddenException("Acesso restrito a administradores");
        }
    }
}
