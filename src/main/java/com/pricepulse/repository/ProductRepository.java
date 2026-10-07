package com.pricepulse.repository;

import com.pricepulse.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByProductUrl(String productUrl);

    boolean existsByProductUrlAndIdNot(String productUrl, Long id);
}
