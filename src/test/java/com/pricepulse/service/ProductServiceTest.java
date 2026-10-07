package com.pricepulse.service;

import com.pricepulse.dto.PriceDropResponse;
import com.pricepulse.entity.Product;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.repository.PriceHistoryRepository;
import com.pricepulse.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository productRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productService = new ProductService(productRepository, mock(PriceHistoryRepository.class));
    }

    @Test
    void targetReachedWhenCurrentPriceIsBelowTarget() {
        givenProduct(1L, "800.00", "1000.00");

        PriceDropResponse response = productService.getPriceDropStatus(1L);

        assertThat(response.targetReached()).isTrue();
        assertThat(response.amountAboveTarget()).isEqualByComparingTo("0");
    }

    @Test
    void targetNotReachedWhenCurrentPriceIsAboveTarget() {
        givenProduct(1L, "1200.00", "1000.00");

        PriceDropResponse response = productService.getPriceDropStatus(1L);

        assertThat(response.targetReached()).isFalse();
        assertThat(response.amountAboveTarget()).isEqualByComparingTo("200.00");
    }

    @Test
    void targetReachedWhenCurrentPriceEqualsTarget() {
        givenProduct(1L, "1000.00", "1000.00");

        PriceDropResponse response = productService.getPriceDropStatus(1L);

        assertThat(response.targetReached()).isTrue();
    }

    @Test
    void throwsWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getPriceDropStatus(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found with id 99");
    }

    private void givenProduct(Long id, String currentPrice, String targetPrice) {
        Product product = new Product("Test product", "https://example.com/test",
                new BigDecimal(currentPrice), new BigDecimal(targetPrice), "INR");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
    }
}
