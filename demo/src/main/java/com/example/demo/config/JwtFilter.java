package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip JWT processing para static assets
        return path.startsWith("/assets/")
                || path.equals("/favicon.ico")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".ico")
                || path.endsWith(".png")
                || path.endsWith(".svg")
                || path.endsWith(".woff")
                || path.endsWith(".woff2");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                if (jwtUtil.isValid(token)) {
                    String email = jwtUtil.extractEmail(token);
                    String role = jwtUtil.extractRole(token);
                    Long userId = jwtUtil.extractUserId(token);

                    var auth = new UsernamePasswordAuthenticationToken(
                            email, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    auth.setDetails(Map.of("userId", userId, "email", email));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    logger.debug("JWT validado para usuário: {}", email);
                } else {
                    logger.warn("Token JWT inválido ou expirado");
                }
            } catch (Exception e) {
                logger.warn("Erro ao processar JWT: {}", e.getMessage());
                // Não falha - deixa o request prosseguir
            }
        }

        chain.doFilter(request, response);
    }
}


