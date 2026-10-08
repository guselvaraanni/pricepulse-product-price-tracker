package com.pricepulse.dto;

import com.pricepulse.entity.PriceMovement;

import java.math.BigDecimal;

// Prices are null when there is no history; direction and change fields are null with fewer than two records.
public record PriceTrendResponse(
        Long productId,
        String currency,
        BigDecimal currentPrice,
        BigDecimal previousPrice,
        BigDecimal lowestPrice,
        BigDecimal highestPrice,
        BigDecimal averagePrice,
        PriceMovement direction,
        BigDecimal changeAmount,
        BigDecimal changePercent) {
}
