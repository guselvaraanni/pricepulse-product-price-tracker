package com.pricepulse.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final int SCALE = 2;

    private MoneyUtils() {
    }

    // UNNECESSARY: money is never silently rounded. Validation already limits input to 2 decimal places.
    public static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
}
