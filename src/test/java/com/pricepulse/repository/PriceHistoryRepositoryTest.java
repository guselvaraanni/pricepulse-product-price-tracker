package com.pricepulse.repository;

import com.pricepulse.entity.PriceHistory;
import com.pricepulse.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Runs against the real PostgreSQL database; each test is rolled back afterwards.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PriceHistoryRepositoryTest {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TestEntityManager entityManager;

    @Autowired
    PriceHistoryRepositoryTest(ProductRepository productRepository,
                               PriceHistoryRepository priceHistoryRepository,
                               TestEntityManager entityManager) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.entityManager = entityManager;
    }

    @Test
    void historyIsNewestFirstAndIdBreaksTimestampTies() {
        Product product = saveProduct();
        LocalDateTime sameTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        PriceHistory older = priceHistoryRepository.save(
                new PriceHistory(product, new BigDecimal("900.00"), sameTime.minusDays(1)));
        PriceHistory first = priceHistoryRepository.save(new PriceHistory(product, new BigDecimal("850.00"), sameTime));
        PriceHistory second = priceHistoryRepository.save(new PriceHistory(product, new BigDecimal("800.00"), sameTime));

        List<PriceHistory> history = priceHistoryRepository.findByProductIdOrderByRecordedAtDescIdDesc(product.getId());

        assertThat(history).extracting(PriceHistory::getId)
                .containsExactly(second.getId(), first.getId(), older.getId());
    }

    @Test
    void deletingProductAlsoDeletesItsHistory() {
        Product product = saveProduct();
        priceHistoryRepository.save(new PriceHistory(product, new BigDecimal("900.00"), LocalDateTime.now()));
        priceHistoryRepository.save(new PriceHistory(product, new BigDecimal("800.00"), LocalDateTime.now()));
        Long productId = product.getId();

        // Clear the persistence context so the product is reloaded with its history from the database,
        // just like a fresh DELETE request would.
        entityManager.flush();
        entityManager.clear();

        productRepository.delete(productRepository.findById(productId).orElseThrow());
        entityManager.flush();

        assertThat(productRepository.existsById(productId)).isFalse();
        assertThat(priceHistoryRepository.findByProductIdOrderByRecordedAtDescIdDesc(productId)).isEmpty();
    }

    private Product saveProduct() {
        return productRepository.save(new Product("Test product", "https://example.com/test-" + UUID.randomUUID(),
                new BigDecimal("1000.00"), new BigDecimal("900.00"), "INR"));
    }
}
