package com.example.demo.repository;

import com.example.demo.model.Product;
import com.example.demo.model.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {
    List<ProductSize> findByProduct(Product product);
    Optional<ProductSize> findByProductAndSize(Product product, String size);

    @Modifying
    @Transactional
    void deleteByProduct(Product product);
}
