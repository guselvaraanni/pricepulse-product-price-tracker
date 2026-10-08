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
import com.pricepulse.util.MoneyUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class PriceAnalysisService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public PriceAnalysisService(ProductRepository productRepository,
                                PriceHistoryRepository priceHistoryRepository) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Transactional(readOnly = true)
    public PriceAnalysisResponse analyzePriceHistory(Long productId, int recentLimit) {
        Product product = findProductOrThrow(productId);
        List<PriceHistory> newestFirst = findHistoryNewestFirst(productId);

        List<BigDecimal> chronologicalPrices = newestFirst.reversed().stream()
                .map(PriceHistory::getPrice)
                .toList();

        Map<PriceMovement, Long> movements = countMovements(chronologicalPrices);

        List<PriceHistoryResponse> recentPrices = newestFirst.stream()
                .limit(recentLimit)
                .map(PriceHistoryResponse::from)
                .toList();

        return new PriceAnalysisResponse(
                product.getId(),
                product.getCurrency(),
                chronologicalPrices.size(),
                lowest(chronologicalPrices).orElse(null),
                highest(chronologicalPrices).orElse(null),
                average(chronologicalPrices).orElse(null),
                movements.getOrDefault(PriceMovement.INCREASE, 0L),
                movements.getOrDefault(PriceMovement.DECREASE, 0L),
                movements.getOrDefault(PriceMovement.UNCHANGED, 0L),
                recentPrices);
    }

    @Transactional(readOnly = true)
    public PriceTrendResponse analyzePriceTrend(Long productId) {
        Product product = findProductOrThrow(productId);
        List<PriceHistory> newestFirst = findHistoryNewestFirst(productId);

        List<BigDecimal> prices = newestFirst.stream()
                .map(PriceHistory::getPrice)
                .toList();

        Optional<BigDecimal> latestPrice = prices.stream().findFirst();
        Optional<BigDecimal> previousPrice = prices.stream().skip(1).findFirst();

        // Present only when both prices exist; otherwise empty, never null.
        Optional<PriceChange> change = previousPrice.flatMap(previous ->
                latestPrice.map(latest -> PriceChange.between(previous, latest)));

        return new PriceTrendResponse(
                product.getId(),
                product.getCurrency(),
                latestPrice.orElse(null),
                previousPrice.orElse(null),
                lowest(prices).orElse(null),
                highest(prices).orElse(null),
                average(prices).orElse(null),
                change.map(PriceChange::direction).orElse(null),
                change.map(PriceChange::amount).orElse(null),
                change.map(PriceChange::percent).orElse(null));
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    // One query, already sorted by the database: no need to sort again in Java.
    private List<PriceHistory> findHistoryNewestFirst(Long productId) {
        return priceHistoryRepository.findByProductIdOrderByRecordedAtDescIdDesc(productId);
    }

    private Optional<BigDecimal> lowest(List<BigDecimal> prices) {
        return prices.stream().min(Comparator.naturalOrder());
    }

    private Optional<BigDecimal> highest(List<BigDecimal> prices) {
        return prices.stream().max(Comparator.naturalOrder());
    }

    private Optional<BigDecimal> average(List<BigDecimal> prices) {
        if (prices.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal total = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        // divide() needs a scale and rounding mode: averages like 301 / 3 do not terminate.
        return Optional.of(total.divide(BigDecimal.valueOf(prices.size()), MoneyUtils.SCALE, RoundingMode.HALF_UP));
    }

    // Compares each price with the one recorded before it: [100, 120, 90] -> {INCREASE=1, DECREASE=1}.
    private Map<PriceMovement, Long> countMovements(List<BigDecimal> chronologicalPrices) {
        return IntStream.range(1, chronologicalPrices.size())
                .mapToObj(i -> PriceMovement.between(chronologicalPrices.get(i - 1), chronologicalPrices.get(i)))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}
