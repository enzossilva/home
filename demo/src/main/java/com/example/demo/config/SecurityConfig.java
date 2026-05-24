package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final RateLimitFilter rateLimitFilter;

    @Value("${app.frontend.url:https://localhost:3000}")
    private String frontendUrl;

    public SecurityConfig(JwtFilter jwtFilter, RateLimitFilter rateLimitFilter) {
        this.jwtFilter = jwtFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentTypeOptions(ct -> {})
                .frameOptions(fo -> fo.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .referrerPolicy(rp -> rp
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .authorizeHttpRequests(auth -> auth
                // Static assets — declared first so they are never caught by anyRequest().authenticated()
                // Spring Security 7.x uses MVC path matching by default, which doesn't correctly handle
                // Ant-style patterns for static asset directories. Use a lambda matcher that checks
                // getServletPath() (not getRequestURI()) so that the match works correctly for both
                // direct requests and internal forwards (e.g. SPA routes forwarded to index.html).
                .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
                .requestMatchers("/login", "/register", "/admin", "/checkout",
                        "/product/**", "/pedido/**", "/meus-pedidos",
                        "/esqueci-senha", "/reset-senha", "/privacidade", "/termos").permitAll()
                .requestMatchers("/users/register", "/users/login",
                        "/users/reset-request", "/users/reset-password").permitAll()
                .requestMatchers("/payment/public-key").permitAll()
                .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/frete/calcular").permitAll()
                .requestMatchers("/orders/webhook/mp").permitAll()

                // Só ADMIN
                .requestMatchers("/orders/admin/all", "/orders/admin/stats").hasRole("ADMIN")
                .requestMatchers("/orders/*/ship", "/orders/*/etiqueta").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/products").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")
                .requestMatchers("/users", "/users/*/role").hasRole("ADMIN")

                // Qualquer usuário autenticado
                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
            .requestMatchers("/assets/**", "/**/*.js", "/**/*.css",
                    "/**/*.ico", "/**/*.png", "/**/*.svg",
                    "/**/*.woff", "/**/*.woff2");
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            frontendUrl,
            "https://localhost:3000",
            "http://localhost:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
