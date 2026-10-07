package com.pricepulse.dto;

import com.pricepulse.entity.PriceHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceHistoryResponse(
        Long id,
        Long productId,
        BigDecimal price,
        LocalDateTime recordedAt) {

    public static PriceHistoryResponse from(PriceHistory priceHistory) {
        return new PriceHistoryResponse(
                priceHistory.getId(),
                // Reading only the id of a lazy proxy does not trigger a database query.
                priceHistory.getProduct().getId(),
                priceHistory.getPrice(),
                priceHistory.getRecordedAt());
    }
}
