package com.pricepulse.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RecordPriceRequest(

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 digits and 2 decimal places")
        BigDecimal price) {
}
