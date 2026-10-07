package com.pricepulse.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 1000)
    private String productUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // REMOVE only: history is created through PriceHistoryRepository, but has no meaning once its product is deleted.
    @OneToMany(mappedBy = "product", cascade = CascadeType.REMOVE)
    private List<PriceHistory> priceHistory = new ArrayList<>();

    // Required by JPA: Hibernate instantiates entities through a no-arg constructor.
    protected Product() {
    }

    public Product(String name, String productUrl, BigDecimal currentPrice,
                   BigDecimal targetPrice, String currency) {
        this.name = name;
        this.productUrl = productUrl;
        this.currentPrice = currentPrice;
        this.targetPrice = targetPrice;
        this.currency = currency;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    // The only way to change the price, so every change can be paired with a PriceHistory record.
    public void updateCurrentPrice(BigDecimal newPrice) {
        this.currentPrice = newPrice;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(BigDecimal targetPrice) {
        this.targetPrice = targetPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Read-only view: callers must not add or remove history through the product.
    public List<PriceHistory> getPriceHistory() {
        return Collections.unmodifiableList(priceHistory);
    }
}
