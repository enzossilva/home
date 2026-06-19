package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.ValidationException;
import com.example.demo.model.Product;
import com.example.demo.model.ProductSize;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSizeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final CartRepository cartRepository;

    public ProductService(ProductRepository productRepository,
                          ProductSizeRepository productSizeRepository,
                          CartRepository cartRepository) {
        this.productRepository = productRepository;
        this.productSizeRepository = productSizeRepository;
        this.cartRepository = cartRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public Product addProduct(Product product) {
        validateProduct(product);

        Map<String, Integer> sizeStocks = product.getSizeStocks();
        product.setSizeStocks(null);

        syncPrimaryImage(product);

        if (sizeStocks != null && !sizeStocks.isEmpty()) {
            int total = sizeStocks.values().stream().mapToInt(v -> v != null ? v : 0).sum();
            product.setStock(total);
        }

        Product saved = productRepository.save(product);
        saveSizes(saved, sizeStocks);
        logger.info("Produto criado: id={}, nome={}", saved.getId(), saved.getName());
        return productRepository.findById(saved.getId()).orElse(saved);
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));

        // Remove itens do carrinho que referenciam este produto
        cartRepository.deleteByProductId(id);
        productRepository.deleteById(id);
        logger.info("Produto deletado: id={}", id);
    }

    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));

        validateProduct(productDetails);

        Map<String, Integer> sizeStocks = productDetails.getSizeStocks();

        product.setName(productDetails.getName());
        product.setPrice(productDetails.getPrice());
        product.setDescription(productDetails.getDescription());
        product.setImages(productDetails.getImages());
        product.setCategory(productDetails.getCategory());
        syncPrimaryImage(product);

        if (sizeStocks != null && !sizeStocks.isEmpty()) {
            int total = sizeStocks.values().stream().mapToInt(v -> v != null ? v : 0).sum();
            product.setStock(total);
        } else {
            product.setStock(productDetails.getStock());
        }

        Product saved = productRepository.save(product);
        saveSizes(saved, sizeStocks);
        logger.info("Produto atualizado: id={}, nome={}", id, saved.getName());
        return productRepository.findById(saved.getId()).orElse(saved);
    }

    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ValidationException("Nome do produto é obrigatório");
        }
        if (product.getPrice() == null || product.getPrice() <= 0) {
            throw new ValidationException("Preço deve ser maior que zero");
        }
    }

    private void syncPrimaryImage(Product product) {
        List<String> images = product.getImages();
        if (images != null && !images.isEmpty()) {
            product.setImageUrl(images.get(0));
        } else if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
            product.setImages(new ArrayList<>(List.of(product.getImageUrl())));
        }
    }

    private void saveSizes(Product product, Map<String, Integer> sizeStocks) {
        productSizeRepository.deleteByProduct(product);
        if (sizeStocks == null || sizeStocks.isEmpty()) return;

        List<ProductSize> sizes = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sizeStocks.entrySet()) {
            int stock = entry.getValue() != null && entry.getValue() > 0 ? entry.getValue() : 0;
            ProductSize ps = new ProductSize();
            ps.setProduct(product);
            ps.setSize(entry.getKey());
            ps.setStock(stock);
            sizes.add(ps);
        }
        productSizeRepository.saveAll(sizes);
    }
}
