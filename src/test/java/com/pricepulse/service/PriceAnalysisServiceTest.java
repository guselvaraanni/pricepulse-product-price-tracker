package com.pricepulse.service;

import com.pricepulse.dto.PriceAnalysisResponse;
import com.pricepulse.dto.PriceHistoryResponse;
import com.pricepulse.dto.PriceTrendResponse;
import com.pricepulse.entity.PriceHistory;
import com.pricepulse.entity.PriceMovement;
import com.pricepulse.entity.Product;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.repository.PriceHistoryRepository;
import com.pricepulse.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriceAnalysisServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 10, 0);

    private ProductRepository productRepository;
    private PriceHistoryRepository priceHistoryRepository;
    private PriceAnalysisService priceAnalysisService;
    private Product product;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        priceHistoryRepository = mock(PriceHistoryRepository.class);
        priceAnalysisService = new PriceAnalysisService(productRepository, priceHistoryRepository);

        product = new Product("Test product", "https://example.com/test",
                new BigDecimal("900.00"), new BigDecimal("850.00"), "INR");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    }

    @Test
    void calculatesStatisticsAndMovements() {
        // Chronological: 1200 -> 1000 (down) -> 1000 (same) -> 800 (down) -> 900 (up)
        givenChronologicalPrices("1200.00", "1000.00", "1000.00", "800.00", "900.00");

        PriceAnalysisResponse analysis = priceAnalysisService.analyzePriceHistory(1L, 5);

        assertThat(analysis.recordCount()).isEqualTo(5);
        assertThat(analysis.lowestPrice()).isEqualByComparingTo("800.00");
        assertThat(analysis.highestPrice()).isEqualByComparingTo("1200.00");
        assertThat(analysis.averagePrice()).isEqualByComparingTo("980.00");
        assertThat(analysis.priceIncreases()).isEqualTo(1);
        assertThat(analysis.priceDecreases()).isEqualTo(2);
        assertThat(analysis.unchangedPrices()).isEqualTo(1);
    }

    @Test
    void recentPricesAreNewestFirstAndLimited() {
        givenChronologicalPrices("1200.00", "1000.00", "800.00", "900.00");

        PriceAnalysisResponse analysis = priceAnalysisService.analyzePriceHistory(1L, 2);

        assertThat(analysis.recentPrices())
                .extracting(PriceHistoryResponse::price)
                .containsExactly(new BigDecimal("900.00"), new BigDecimal("800.00"));
    }

    @Test
    void averageIsRoundedHalfUpToTwoDecimals() {
        givenChronologicalPrices("100.00", "100.00", "101.00");

        PriceAnalysisResponse analysis = priceAnalysisService.analyzePriceHistory(1L, 5);

        // 301 / 3 = 100.333... -> 100.33
        assertThat(analysis.averagePrice()).isEqualByComparingTo("100.33");
    }

    @Test
    void emptyHistoryReturnsZeroCountsAndNoStatistics() {
        givenChronologicalPrices();

        PriceAnalysisResponse analysis = priceAnalysisService.analyzePriceHistory(1L, 5);

        assertThat(analysis.recordCount()).isZero();
        assertThat(analysis.lowestPrice()).isNull();
        assertThat(analysis.highestPrice()).isNull();
        assertThat(analysis.averagePrice()).isNull();
        assertThat(analysis.priceIncreases()).isZero();
        assertThat(analysis.recentPrices()).isEmpty();
    }

    @Test
    void trendShowsDecreaseFromPreviousPrice() {
        givenChronologicalPrices("1200.00", "1000.00", "800.00");

        PriceTrendResponse trend = priceAnalysisService.analyzePriceTrend(1L);

        assertThat(trend.currentPrice()).isEqualByComparingTo("800.00");
        assertThat(trend.previousPrice()).isEqualByComparingTo("1000.00");
        assertThat(trend.direction()).isEqualTo(PriceMovement.DECREASE);
        assertThat(trend.changeAmount()).isEqualByComparingTo("-200.00");
        assertThat(trend.changePercent()).isEqualByComparingTo("-20.00");
        assertThat(trend.lowestPrice()).isEqualByComparingTo("800.00");
        assertThat(trend.highestPrice()).isEqualByComparingTo("1200.00");
        assertThat(trend.averagePrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void trendShowsIncreaseFromPreviousPrice() {
        givenChronologicalPrices("800.00", "900.00");

        PriceTrendResponse trend = priceAnalysisService.analyzePriceTrend(1L);

        assertThat(trend.direction()).isEqualTo(PriceMovement.INCREASE);
        assertThat(trend.changeAmount()).isEqualByComparingTo("100.00");
        assertThat(trend.changePercent()).isEqualByComparingTo("12.50");
    }

    @Test
    void trendShowsUnchangedWhenLatestEqualsPrevious() {
        givenChronologicalPrices("1000.00", "1000.00");

        PriceTrendResponse trend = priceAnalysisService.analyzePriceTrend(1L);

        assertThat(trend.direction()).isEqualTo(PriceMovement.UNCHANGED);
        assertThat(trend.changePercent()).isEqualByComparingTo("0");
    }

    @Test
    void trendPercentIsRoundedHalfUpToTwoDecimals() {
        givenChronologicalPrices("3.00", "2.00");

        PriceTrendResponse trend = priceAnalysisService.analyzePriceTrend(1L);

        // -1 / 3 * 100 = -33.333... -> -33.33
        assertThat(trend.changePercent()).isEqualByComparingTo("-33.33");
    }

    @Test
    void trendWithSingleRecordHasNoPreviousPriceOrChange() {
        givenChronologicalPrices("1000.00");

        PriceTrendResponse trend = priceAnalysisService.analyzePriceTrend(1L);

        assertThat(trend.currentPrice()).isEqualByComparingTo("1000.00");
        assertThat(trend.previousPrice()).isNull();
        assertThat(trend.direction()).isNull();
        assertThat(trend.changeAmount()).isNull();
        assertThat(trend.changePercent()).isNull();
        assertThat(trend.averagePrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void trendWithEmptyHistoryHasNoPrices() {
        givenChronologicalPrices();

        PriceTrendResponse trend = priceAnalysisService.analyzePriceTrend(1L);

        assertThat(trend.currentPrice()).isNull();
        assertThat(trend.previousPrice()).isNull();
        assertThat(trend.lowestPrice()).isNull();
        assertThat(trend.direction()).isNull();
    }

    @Test
    void throwsWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceAnalysisService.analyzePriceHistory(99L, 5))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // The repository returns newest first, so build the chronological list and reverse it.
    private void givenChronologicalPrices(String... prices) {
        List<PriceHistory> history = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            history.add(new PriceHistory(product, new BigDecimal(prices[i]), START.plusDays(i)));
        }
        when(priceHistoryRepository.findByProductIdOrderByRecordedAtDescIdDesc(1L)).thenReturn(history.reversed());
    }
}
