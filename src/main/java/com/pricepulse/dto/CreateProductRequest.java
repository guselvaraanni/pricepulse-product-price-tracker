package com.pricepulse.dto;

import java.math.BigDecimal;

public record CreateProductRequest(
        String name,
        String productUrl,
        BigDecimal currentPrice,
        BigDecimal targetPrice,
        String currency) {
}
