package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSizeRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final EmailService emailService;
    private final EtiquetaService etiquetaService;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        CartService cartService,
                        ProductRepository productRepository,
                        ProductSizeRepository productSizeRepository,
                        EmailService emailService,
                        EtiquetaService etiquetaService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.productSizeRepository = productSizeRepository;
        this.emailService = emailService;
        this.etiquetaService = etiquetaService;
    }

    public Map<String, String> gerarEtiqueta(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        Map<String, String> result = etiquetaService.gerarEtiqueta(order);

        // Salva tracking e marca como SHIPPED
        order.setTrackingCode(result.get("trackingCode"));
        order.setStatus("SHIPPED");
        orderRepository.save(order);

        // Email para o cliente
        emailService.enviarCodigoRastreio(order);

        return result;
    }

    public Order createOrder(Long userId, Map<String, String> address,
                             Double shippingCost, String shippingMethod) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<Cart> cartItems = cartService.getCartByUser(userId);
        if (cartItems.isEmpty()) throw new RuntimeException("Carrinho vazio");

        Double subtotal = cartItems.stream()
                .mapToDouble(Cart::getSubtotal)
                .sum();

        Order order = new Order();
        order.setUser(user);
        order.setSubtotal(subtotal);
        order.setShippingCost(shippingCost);
        order.setTotal(subtotal + shippingCost);
        order.setShippingMethod(shippingMethod);
        order.setCep(address.get("cep"));
        order.setRua(address.get("rua"));
        order.setNumero(address.get("numero"));
        order.setComplemento(address.get("complemento"));
        order.setBairro(address.get("bairro"));
        order.setCidade(address.get("cidade"));
        order.setEstado(address.get("estado"));

        Order saved = orderRepository.save(order);

        List<OrderItem> items = cartItems.stream().map(cart -> {
            OrderItem item = new OrderItem();
            item.setOrder(saved);
            item.setProductId(cart.getProduct().getId());
            item.setProductName(cart.getProduct().getName());
            item.setProductImage(cart.getProduct().getImageUrl());
            item.setProductPrice(cart.getProduct().getPrice());
            item.setSize(cart.getSize());
            item.setQuantity(cart.getQuantity());
            return item;
        }).collect(Collectors.toList());

        saved.setItems(items);
        Order finalOrder = orderRepository.save(saved);

        // Email de confirmação
        emailService.enviarConfirmacaoPedido(finalOrder);

        return finalOrder;
    }

    @Transactional
    public void markAsPaid(Long orderId, String mpPaymentId) {
        markAsPaidWithCpf(orderId, mpPaymentId, null);
    }

    @Transactional
    public void markAsPaidWithCpf(Long orderId, String mpPaymentId, String buyerCpf) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        order.setStatus("PAID");
        order.setMpPaymentId(mpPaymentId);
        if (buyerCpf != null && !buyerCpf.isBlank()) {
            order.setBuyerCpf(buyerCpf.replaceAll("[^0-9]", ""));
        }
        orderRepository.save(order);

        // Limpa carrinho
        cartService.clearCart(order.getUser().getId());

        // Decrementa estoque por tamanho (ou estoque geral se sem tamanho)
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getProductId() == null) continue;
                productRepository.findById(item.getProductId()).ifPresent(product -> {
                    if (item.getSize() != null && !item.getSize().isBlank()) {
                        productSizeRepository.findByProductAndSize(product, item.getSize()).ifPresent(ps -> {
                            ps.setStock(Math.max(0, ps.getStock() - item.getQuantity()));
                            productSizeRepository.save(ps);
                            // Atualiza total do produto
                            int total = productSizeRepository.findByProduct(product)
                                    .stream().mapToInt(s -> s.getStock() != null ? s.getStock() : 0).sum();
                            product.setStock(total);
                            productRepository.save(product);
                        });
                    } else {
                        product.setStock(Math.max(0, product.getStock() - item.getQuantity()));
                        productRepository.save(product);
                    }
                });
            }
        }
    }

    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Sem permissão para cancelar este pedido");
        }
        if ("SHIPPED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("Pedido já enviado não pode ser cancelado");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("Pedido já está cancelado");
        }

        boolean wasPaid = "PAID".equals(order.getStatus());

        order.setStatus("CANCELLED");
        orderRepository.save(order);

        // Devolve estoque se o pedido estava pago
        if (wasPaid && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getProductId() == null) continue;
                productRepository.findById(item.getProductId()).ifPresent(product -> {
                    if (item.getSize() != null && !item.getSize().isBlank()) {
                        productSizeRepository.findByProductAndSize(product, item.getSize()).ifPresent(ps -> {
                            ps.setStock(ps.getStock() + item.getQuantity());
                            productSizeRepository.save(ps);
                        });
                    } else {
                        product.setStock(product.getStock() + item.getQuantity());
                        productRepository.save(product);
                    }
                });
            }
        }
    }

    @Transactional
    public void setBuyerCpf(Long orderId, String cpf) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        order.setBuyerCpf(cpf);
        orderRepository.save(order);
    }

    public void markAsShipped(Long orderId, String trackingCode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        order.setStatus("SHIPPED");
        order.setTrackingCode(trackingCode);
        orderRepository.save(order);
        emailService.enviarCodigoRastreio(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    }

    public List<Order> getOrdersByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Calcula o frete base pelo CEP:
     * SP: 01000-19999 → R$15
     * Sul/Sudeste (RJ/ES/MG): 20000-39999 → R$20
     * Sul (PR/SC/RS): 80000-99999 → R$18
     * Nordeste: 40000-65999 → R$25
     * Centro-Oeste: 66000-79999 → R$25
     * Norte: restante → R$35
     * EXPRESS adiciona R$15
     */
    public static double calcularFrete(String cep, String method) {
        String digits = cep.replaceAll("[^0-9]", "");
        if (digits.length() < 5) return 30.0;
        int prefix = Integer.parseInt(digits.substring(0, 5));

        double base;
        if (prefix <= 19999) base = 15.0;
        else if (prefix <= 39999) base = 20.0;
        else if (prefix <= 65999) base = 25.0;
        else if (prefix <= 79999) base = 25.0;
        else if (prefix <= 99999) base = 18.0;
        else base = 35.0;

        return "SEDEX".equals(method) ? base + 15.0 : base;
    }
}
