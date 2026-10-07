package com.pricepulse.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyUtilsTest {

    @Test
    void normalizesWholeNumberToTwoDecimalPlaces() {
        BigDecimal normalized = MoneyUtils.normalize(new BigDecimal("800"));

        assertThat(normalized.scale()).isEqualTo(2);
        assertThat(normalized).hasToString("800.00");
    }

    @Test
    void neverSilentlyRoundsExtraDecimalPlaces() {
        assertThatThrownBy(() -> MoneyUtils.normalize(new BigDecimal("10.999")))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void equalsComparesScaleButCompareToDoesNot() {
        BigDecimal oneDecimal = new BigDecimal("1000.0");
        BigDecimal twoDecimals = new BigDecimal("1000.00");

        assertThat(oneDecimal.equals(twoDecimals)).isFalse();
        assertThat(oneDecimal.compareTo(twoDecimals)).isZero();
    }
}
