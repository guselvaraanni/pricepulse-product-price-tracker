package com.pricepulse.dto;

import com.pricepulse.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String productUrl,
        BigDecimal currentPrice,
        BigDecimal targetPrice,
        String currency,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getProductUrl(),
                product.getCurrentPrice(),
                product.getTargetPrice(),
                product.getCurrency(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
