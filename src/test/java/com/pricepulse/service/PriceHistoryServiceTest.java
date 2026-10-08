package com.pricepulse.service;

import com.pricepulse.dto.PriceHistoryResponse;
import com.pricepulse.dto.RecordPriceRequest;
import com.pricepulse.entity.PriceHistory;
import com.pricepulse.entity.Product;
import com.pricepulse.exception.InactiveProductException;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.repository.PriceHistoryRepository;
import com.pricepulse.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceHistoryServiceTest {

    private ProductRepository productRepository;
    private PriceHistoryRepository priceHistoryRepository;
    private PriceHistoryService priceHistoryService;
    private Product product;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        priceHistoryRepository = mock(PriceHistoryRepository.class);
        priceHistoryService = new PriceHistoryService(priceHistoryRepository, productRepository);

        product = new Product("Test product", "https://example.com/test",
                new BigDecimal("1200.00"), new BigDecimal("1000.00"), "INR");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        // save() returns the entity it was given, like the real repository does for a new entity.
        when(priceHistoryRepository.save(any(PriceHistory.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void recordingPriceSavesHistoryAndUpdatesCurrentPrice() {
        PriceHistoryResponse response = priceHistoryService.recordPrice(1L, new RecordPriceRequest(new BigDecimal("800")));

        assertThat(response.price()).isEqualByComparingTo("800.00");
        assertThat(product.getCurrentPrice()).isEqualByComparingTo("800.00");
        verify(priceHistoryRepository).save(any(PriceHistory.class));
    }

    @Test
    void recordingPriceOnInactiveProductIsRejectedAndNothingIsSaved() {
        product.setActive(false);

        assertThatThrownBy(() -> priceHistoryService.recordPrice(1L, new RecordPriceRequest(new BigDecimal("800"))))
                .isInstanceOf(InactiveProductException.class);

        assertThat(product.getCurrentPrice()).isEqualByComparingTo("1200.00");
        verify(priceHistoryRepository, never()).save(any(PriceHistory.class));
    }

    @Test
    void recordingPriceForUnknownProductThrowsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceHistoryService.recordPrice(99L, new RecordPriceRequest(new BigDecimal("800"))))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
