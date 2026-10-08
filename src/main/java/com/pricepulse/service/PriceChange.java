package com.pricepulse.service;

import com.pricepulse.entity.PriceMovement;
import com.pricepulse.util.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

record PriceChange(PriceMovement direction, BigDecimal amount, BigDecimal percent) {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    // previousPrice is always positive (validated on input), so the division is always defined.
    static PriceChange between(BigDecimal previousPrice, BigDecimal latestPrice) {
        BigDecimal amount = latestPrice.subtract(previousPrice);
        // Multiply before dividing so no precision is lost before the final rounding.
        BigDecimal percent = amount.multiply(ONE_HUNDRED)
                .divide(previousPrice, MoneyUtils.SCALE, RoundingMode.HALF_UP);
        return new PriceChange(PriceMovement.between(previousPrice, latestPrice), amount, percent);
    }
}
