package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.ProductSize;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSizeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

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

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public Product addProduct(Product product) {
        Map<String, Integer> sizeStocks = product.getSizeStocks();
        product.setSizeStocks(null);

        // Estoque total = soma dos tamanhos (ou direto se sem tamanhos)
        if (sizeStocks != null && !sizeStocks.isEmpty()) {
            int total = sizeStocks.values().stream().mapToInt(v -> v != null ? v : 0).sum();
            product.setStock(total);
        }

        Product saved = productRepository.save(product);
        saveSizes(saved, sizeStocks);
        return productRepository.findById(saved.getId()).orElse(saved);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteProduct(Long id) {
        // Remove itens do carrinho que referenciam este produto
        cartRepository.deleteByProductId(id);
        productRepository.deleteById(id);
    }

    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;

        Map<String, Integer> sizeStocks = productDetails.getSizeStocks();

        product.setName(productDetails.getName());
        product.setPrice(productDetails.getPrice());
        product.setDescription(productDetails.getDescription());
        product.setImageUrl(productDetails.getImageUrl());
        product.setCategory(productDetails.getCategory());

        if (sizeStocks != null && !sizeStocks.isEmpty()) {
            int total = sizeStocks.values().stream().mapToInt(v -> v != null ? v : 0).sum();
            product.setStock(total);
        } else {
            product.setStock(productDetails.getStock());
        }

        Product saved = productRepository.save(product);
        saveSizes(saved, sizeStocks);
        return productRepository.findById(saved.getId()).orElse(saved);
    }

    private void saveSizes(Product product, Map<String, Integer> sizeStocks) {
        productSizeRepository.deleteByProduct(product);
        if (sizeStocks == null || sizeStocks.isEmpty()) return;

        List<ProductSize> sizes = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sizeStocks.entrySet()) {
            int stock = entry.getValue() != null ? entry.getValue() : 0;
            ProductSize ps = new ProductSize();
            ps.setProduct(product);
            ps.setSize(entry.getKey());
            ps.setStock(stock);
            sizes.add(ps);
        }
        productSizeRepository.saveAll(sizes);
    }
}
