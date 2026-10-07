package com.pricepulse.service;

import com.pricepulse.dto.PriceAnalysisResponse;
import com.pricepulse.dto.PriceHistoryResponse;
import com.pricepulse.entity.PriceHistory;
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
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        // One query, already sorted by the database (newest first).
        List<PriceHistory> newestFirst = priceHistoryRepository.findByProductIdOrderByRecordedAtDesc(productId);

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
                chronologicalPrices.stream().min(Comparator.naturalOrder()).orElse(null),
                chronologicalPrices.stream().max(Comparator.naturalOrder()).orElse(null),
                average(chronologicalPrices).orElse(null),
                movements.getOrDefault(PriceMovement.INCREASE, 0L),
                movements.getOrDefault(PriceMovement.DECREASE, 0L),
                movements.getOrDefault(PriceMovement.UNCHANGED, 0L),
                recentPrices);
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
