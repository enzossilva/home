package com.example.demo.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Redireciona rotas do React (SPA) para index.html.
 * Rotas de API (/products, /users, etc.) são tratadas pelos @RestControllers
 * antes de chegar aqui.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/login", "/register", "/admin", "/checkout",
        "/product/**", "/pedido/**", "/meus-pedidos",
        "/lookbook", "/videos", "/perfil", "/esqueci-senha", "/reset-senha",
        "/privacidade", "/termos"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
