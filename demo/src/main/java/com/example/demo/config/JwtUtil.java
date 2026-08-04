package com.example.demo.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /** Quanto tempo após expirar ainda dá para renovar a sessão (ms). Default: 30 dias. */
    @Value("${jwt.refresh-window-ms:2592000000}")
    private long refreshWindowMs;

    public String generateToken(String email, String role, Long userId) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Long extractUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    public boolean isValid(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Token com assinatura válida e ainda dentro da janela de renovação
     * (válido agora OU expirado há menos de refreshWindowMs).
     */
    public boolean canRefresh(String token) {
        try {
            Claims claims = parseClaimsAllowExpired(token);
            Date exp = claims.getExpiration();
            if (exp == null) return false;
            long skew = System.currentTimeMillis() - exp.getTime();
            return skew <= refreshWindowMs;
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserIdAllowExpired(String token) {
        return parseClaimsAllowExpired(token).get("userId", Long.class);
    }

    public String extractEmailAllowExpired(String token) {
        return parseClaimsAllowExpired(token).getSubject();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Claims parseClaimsAllowExpired(String token) {
        try {
            return getClaims(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
