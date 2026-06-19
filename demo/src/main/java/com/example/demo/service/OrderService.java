package com.example.demo.service;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSizeRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.LogSafeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

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

    @Transactional
    public Map<String, String> gerarEtiqueta(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderId));

        if (!("PAID".equals(order.getStatus()) || "SHIPPED".equals(order.getStatus()))) {
            throw new BusinessException("Apenas pedidos pagos podem gerar etiqueta");
        }

        Map<String, String> result = etiquetaService.gerarEtiqueta(order);

        // Salva tracking e marca como SHIPPED
        order.setTrackingCode(result.get("trackingCode"));
        order.setStatus("SHIPPED");
        orderRepository.save(order);

        // Email para o cliente
        try {
            emailService.enviarCodigoRastreio(order);
        } catch (Exception e) {
            logger.error("Erro ao enviar email de rastreio para order {}", orderId, e);
            // Não falha a operação se email não puder ser enviado
        }

        return result;
    }

    @Transactional
    public Order createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));

        List<Cart> cartItems = cartService.getCartByUser(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException("Carrinho está vazio");
        }

        Double subtotal = cartItems.stream()
                .mapToDouble(Cart::getSubtotal)
                .sum();

        Double shippingCost = request.getShippingCost() != null ? request.getShippingCost()
                : calcularFrete(request.getAddress().getCep(), request.getShippingMethod());

        Order order = new Order();
        order.setUser(user);
        order.setSubtotal(subtotal);
        order.setShippingCost(shippingCost);
        order.setTotal(subtotal + shippingCost);
        order.setShippingMethod(request.getShippingMethod());

        // Mapeia endereço
        order.setCep(request.getAddress().getCep());
        order.setRua(request.getAddress().getRua());
        order.setNumero(request.getAddress().getNumero());
        order.setComplemento(request.getAddress().getComplemento());
        order.setBairro(request.getAddress().getBairro());
        order.setCidade(request.getAddress().getCidade());
        order.setEstado(request.getAddress().getEstado());

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

        logger.info("Pedido criado: orderId={}, userId={}, total={}", finalOrder.getId(), userId, finalOrder.getTotal());

        // Email de confirmação
        try {
            emailService.enviarConfirmacaoPedido(finalOrder);
        } catch (Exception e) {
            logger.error("Erro ao enviar email de confirmação para order {}", finalOrder.getId(), e);
        }

        return finalOrder;
    }

    @Transactional
    public void markAsPaid(Long orderId, String mpPaymentId) {
        markAsPaidWithCpf(orderId, mpPaymentId, null);
    }

    @Transactional
    public void markAsPaidWithCpf(Long orderId, String mpPaymentId, String buyerCpf) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderId));

        // Proteção contra pagamento duplicado
        if ("PAID".equals(order.getStatus()) || "SHIPPED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())) {
            logger.warn("Tentativa de marcar como pago um pedido que já foi pago: orderId={}", orderId);
            throw new BusinessException("Pedido já foi marcado como pago");
        }

        order.setStatus("PAID");
        order.setMpPaymentId(mpPaymentId);
        if (buyerCpf != null && !buyerCpf.isBlank()) {
            order.setBuyerCpf(buyerCpf.replaceAll("[^0-9]", ""));
        }
        orderRepository.save(order);

        // Limpa carrinho
        cartService.clearCart(order.getUser().getId());

        // Decrementa estoque
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getProductId() == null) continue;
                productRepository.findById(item.getProductId()).ifPresent(product -> {
                    if (item.getSize() != null && !item.getSize().isBlank()) {
                        productSizeRepository.findByProductAndSize(product, item.getSize()).ifPresent(ps -> {
                            int newStock = Math.max(0, (ps.getStock() != null ? ps.getStock() : 0) - item.getQuantity());
                            ps.setStock(newStock);
                            productSizeRepository.save(ps);
                            // Atualiza total
                            int total = productSizeRepository.findByProduct(product)
                                    .stream().mapToInt(s -> s.getStock() != null ? s.getStock() : 0).sum();
                            product.setStock(total);
                            productRepository.save(product);
                        });
                    } else {
                        int newStock = Math.max(0, (product.getStock() != null ? product.getStock() : 0) - item.getQuantity());
                        product.setStock(newStock);
                        productRepository.save(product);
                    }
                });
            }
        }

        logger.info("Pedido marcado como pago: orderId={}", orderId);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Você não pode cancelar o pedido de outro usuário");
        }

        if (!order.getStatus().equals("PENDING") && !order.getStatus().equals("PAID")) {
            throw new BusinessException("Pedidos já enviados não podem ser cancelados");
        }

        boolean wasPaid = "PAID".equals(order.getStatus());

        order.setStatus("CANCELLED");
        orderRepository.save(order);

        // Devolve estoque se foi pago
        if (wasPaid && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getProductId() == null) continue;
                productRepository.findById(item.getProductId()).ifPresent(product -> {
                    if (item.getSize() != null && !item.getSize().isBlank()) {
                        productSizeRepository.findByProductAndSize(product, item.getSize()).ifPresent(ps -> {
                            ps.setStock((ps.getStock() != null ? ps.getStock() : 0) + item.getQuantity());
                            productSizeRepository.save(ps);
                        });
                    } else {
                        product.setStock((product.getStock() != null ? product.getStock() : 0) + item.getQuantity());
                        productRepository.save(product);
                    }
                });
            }
        }

        logger.info("Pedido cancelado: orderId={}", orderId);
    }

    @Transactional
    public void setBuyerCpf(Long orderId, String cpf) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderId));
        order.setBuyerCpf(cpf.replaceAll("[^0-9]", ""));
        orderRepository.save(order);
        logger.info("CPF definido para pedido: orderId={}, cpf_masked={}", orderId, LogSafeUtils.maskCPF(cpf));
    }

    @Transactional
    public void markAsShipped(Long orderId, String trackingCode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderId));
        order.setStatus("SHIPPED");
        order.setTrackingCode(trackingCode);
        orderRepository.save(order);

        try {
            emailService.enviarCodigoRastreio(order);
        } catch (Exception e) {
            logger.error("Erro ao enviar email de rastreio", e);
        }

        logger.info("Pedido marcado como enviado: orderId={}, tracking={}", orderId, trackingCode);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        List<Order> all = getAllOrders();
        LocalDate today = LocalDate.now();

        long todayCount = all.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(today))
                .count();

        double totalRevenue = all.stream()
                .filter(o -> isPaidOrder(o))
                .mapToDouble(o -> o.getTotal() != null ? o.getTotal() : 0)
                .sum();

        long paidCount = all.stream()
                .filter(this::isPaidOrder)
                .count();

        double avgTicket = paidCount > 0 ? totalRevenue / paidCount : 0;

        long pendingCount = all.stream()
                .filter(o -> "PENDING".equals(o.getStatus()))
                .count();

        long awaitingShipmentCount = all.stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", all.size());
        stats.put("ordersToday", todayCount);
        stats.put("totalRevenue", totalRevenue);
        stats.put("averageTicket", avgTicket);
        stats.put("pendingOrders", pendingCount);
        stats.put("awaitingShipment", awaitingShipmentCount);

        return stats;
    }

    @Transactional
    public void processWebhookMP(Map<String, Object> body) {
        try {
            String action = (String) body.getOrDefault("action", "");

            if ("payment.updated".equals(action) || "payment.created".equals(action)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");

                if (data != null && data.containsKey("id")) {
                    String mpId = data.get("id").toString();

                    // Encontra pedido pendente com este ID
                    orderRepository.findAll().stream()
                            .filter(o -> mpId.equals(o.getMpPaymentId()) && "PENDING".equals(o.getStatus()))
                            .findFirst()
                            .ifPresent(o -> markAsPaid(o.getId(), mpId));

                    logger.info("Webhook processado: mpPaymentId={}", mpId);
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao processar webhook MP", e);
            // Webhook sempre retorna 200 para evitar retentativas
        }
    }

    /**
     * Calcula o frete base pelo CEP (Brasil).
     */
    public static double calcularFrete(String cep, String method) {
        String digits = cep.replaceAll("[^0-9]", "");
        if (digits.length() < 5) return 30.0;

        int prefix;
        try {
            prefix = Integer.parseInt(digits.substring(0, 5));
        } catch (NumberFormatException e) {
            return 30.0;
        }

        double base;
        if (prefix <= 19999) base = 15.0;          // São Paulo
        else if (prefix <= 39999) base = 20.0;      // Sudeste
        else if (prefix <= 65999) base = 25.0;      // Nordeste
        else if (prefix <= 79999) base = 25.0;      // Centro-Oeste
        else if (prefix <= 99999) base = 18.0;      // Sul
        else base = 35.0;                            // Norte

        return "SEDEX".equalsIgnoreCase(method) ? base + 15.0 : base;
    }

    private boolean isPaidOrder(Order order) {
        return "PAID".equals(order.getStatus()) || "SHIPPED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus());
    }
}
