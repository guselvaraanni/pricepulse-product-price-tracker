package com.pricepulse.dto;

import java.math.BigDecimal;

// currentPrice is intentionally absent: price changes go through price recording, not product updates.
public record UpdateProductRequest(
        String name,
        String productUrl,
        BigDecimal targetPrice,
        String currency,
        Boolean active) {
}
