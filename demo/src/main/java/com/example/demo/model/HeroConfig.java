package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "hero_config")
public class HeroConfig {

    @Id
    private Long id = 1L;

    @Column(length = 1000)
    private String imageUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
