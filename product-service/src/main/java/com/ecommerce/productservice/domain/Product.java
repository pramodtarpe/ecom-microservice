package com.ecommerce.productservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.Locale;

@Entity
@Table(
        name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"),
        indexes = @Index(name = "idx_products_active", columnList = "active")
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2_000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    protected Product() {
        // Required by JPA.
    }

    public Product(String sku, String name, String description, BigDecimal price, boolean active) {
        update(sku, name, description, price, active);
    }

    public void update(String sku, String name, String description, BigDecimal price, boolean active) {
        this.sku = normalizeSku(sku);
        this.name = name.strip();
        this.description = normalizeDescription(description);
        this.price = price;
        this.active = active;
    }

    @PrePersist
    @PreUpdate
    void normalizePersistentFields() {
        sku = normalizeSku(sku);
        name = name.strip();
        description = normalizeDescription(description);
    }

    public static String normalizeSku(String sku) {
        return sku.strip().toUpperCase(Locale.ROOT);
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }
}
