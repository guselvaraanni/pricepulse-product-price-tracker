package com.pricepulse.dto;

import java.math.BigDecimal;

public record PriceDropResponse(
        Long productId,
        BigDecimal currentPrice,
        BigDecimal targetPrice,
        String currency,
        boolean targetReached,
        BigDecimal amountAboveTarget) {
}
