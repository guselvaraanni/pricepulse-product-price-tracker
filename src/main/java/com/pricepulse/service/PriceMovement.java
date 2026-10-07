package com.pricepulse.service;

import java.math.BigDecimal;

public enum PriceMovement {
    INCREASE,
    DECREASE,
    UNCHANGED;

    public static PriceMovement between(BigDecimal previousPrice, BigDecimal currentPrice) {
        int comparison = currentPrice.compareTo(previousPrice);
        if (comparison > 0) {
            return INCREASE;
        }
        if (comparison < 0) {
            return DECREASE;
        }
        return UNCHANGED;
    }
}
