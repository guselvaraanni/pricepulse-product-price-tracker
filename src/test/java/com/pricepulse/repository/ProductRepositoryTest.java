package com.pricepulse.repository;

import com.pricepulse.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    private final ProductRepository productRepository;

    // Test classes are created by JUnit, not Spring, so @Autowired is required on the constructor here.
    @Autowired
    ProductRepositoryTest(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Test
    void saveAndFindById() {
        // Unique URL so the test never collides with real rows in the shared development database.
        Product product = new Product(
                "Sony WH-1000XM5 Headphones",
                "https://example.com/test-" + UUID.randomUUID(),
                new BigDecimal("29990.00"),
                new BigDecimal("25000.00"),
                "INR");

        Product saved = productRepository.save(product);
        Optional<Product> found = productRepository.findById(saved.getId());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getCurrentPrice()).isEqualByComparingTo("29990.00");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void findByIdReturnsEmptyOptionalWhenMissing() {
        Optional<Product> found = productRepository.findById(-1L);

        assertThat(found).isEmpty();
    }
}
