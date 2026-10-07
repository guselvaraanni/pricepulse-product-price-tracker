package com.pricepulse.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "Product name must not be blank")
        @Size(max = 200, message = "Product name must be at most 200 characters")
        String name,

        @NotBlank(message = "Product URL must not be blank")
        @Size(max = 1000, message = "Product URL must be at most 1000 characters")
        @Pattern(regexp = "^https?://\\S+$", message = "Product URL must start with http:// or https://")
        String productUrl,

        @NotNull(message = "Current price is required")
        @Positive(message = "Current price must be positive")
        @Digits(integer = 10, fraction = 2,
                message = "Current price must have at most 10 digits and 2 decimal places")
        BigDecimal currentPrice,

        @NotNull(message = "Target price is required")
        @Positive(message = "Target price must be positive")
        @Digits(integer = 10, fraction = 2,
                message = "Target price must have at most 10 digits and 2 decimal places")
        BigDecimal targetPrice,

        @NotBlank(message = "Currency must not be blank")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase ISO code such as INR")
        String currency) {
}
