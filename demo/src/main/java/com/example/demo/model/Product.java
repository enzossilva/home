package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.List;
import java.util.Map;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double price;

    private String description;

    private String imageUrl; // foto principal (backward compat)

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", length = 2000)
    @OrderColumn(name = "ordem")
    private List<String> images;

    @Column(nullable = false)
    private Integer stock = 0;

    private String category;

    // Lista de tamanhos com estoque individual
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ProductSize> productSizes;

    // Campo transiente: recebe o mapa { "P": 5, "M": 3 } do frontend
    @Transient
    private Map<String, Integer> sizeStocks;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<ProductSize> getProductSizes() { return productSizes; }
    public void setProductSizes(List<ProductSize> productSizes) { this.productSizes = productSizes; }

    public Map<String, Integer> getSizeStocks() { return sizeStocks; }
    public void setSizeStocks(Map<String, Integer> sizeStocks) { this.sizeStocks = sizeStocks; }
}
