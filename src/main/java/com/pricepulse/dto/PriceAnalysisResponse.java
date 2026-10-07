package com.pricepulse.dto;

import java.math.BigDecimal;
import java.util.List;

// Price statistics are null when the product has no recorded prices yet.
public record PriceAnalysisResponse(
        Long productId,
        String currency,
        long recordCount,
        BigDecimal lowestPrice,
        BigDecimal highestPrice,
        BigDecimal averagePrice,
        long priceIncreases,
        long priceDecreases,
        long unchangedPrices,
        List<PriceHistoryResponse> recentPrices) {
}
