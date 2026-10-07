package com.pricepulse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_history",
        indexes = @Index(name = "idx_price_history_product_recorded_at", columnList = "product_id, recorded_at"))
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    // Required by JPA: Hibernate instantiates entities through a no-arg constructor.
    protected PriceHistory() {
    }

    public PriceHistory(Product product, BigDecimal price, LocalDateTime recordedAt) {
        this.product = product;
        this.price = price;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
