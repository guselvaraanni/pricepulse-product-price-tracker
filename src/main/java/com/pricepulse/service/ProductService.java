package com.pricepulse.service;

import com.pricepulse.dto.CreateProductRequest;
import com.pricepulse.dto.PriceDropResponse;
import com.pricepulse.dto.ProductResponse;
import com.pricepulse.dto.UpdateProductRequest;
import com.pricepulse.entity.PriceHistory;
import com.pricepulse.entity.Product;
import com.pricepulse.exception.DuplicateProductUrlException;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.repository.PriceHistoryRepository;
import com.pricepulse.repository.ProductRepository;
import com.pricepulse.util.MoneyUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public ProductService(ProductRepository productRepository,
                          PriceHistoryRepository priceHistoryRepository) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    // The initial price is also the first history entry, so the history always explains the current price.
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsByProductUrl(request.productUrl())) {
            throw new DuplicateProductUrlException(request.productUrl());
        }

        Product product = new Product(
                request.name(),
                request.productUrl(),
                request.currentPrice(),
                request.targetPrice(),
                request.currency());
        Product saved = productRepository.save(product);

        priceHistoryRepository.save(new PriceHistory(saved, saved.getCurrentPrice(), saved.getCreatedAt()));

        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll(Sort.by("id")).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findProductOrThrow(id));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = findProductOrThrow(id);

        if (productRepository.existsByProductUrlAndIdNot(request.productUrl(), id)) {
            throw new DuplicateProductUrlException(request.productUrl());
        }

        product.setName(request.name());
        product.setProductUrl(request.productUrl());
        product.setTargetPrice(request.targetPrice());
        product.setActive(request.active());

        // Flush now so @PreUpdate sets updatedAt before we build the response.
        return ProductResponse.from(productRepository.saveAndFlush(product));
    }

    @Transactional(readOnly = true)
    public PriceDropResponse getPriceDropStatus(Long id) {
        Product product = findProductOrThrow(id);
        BigDecimal currentPrice = product.getCurrentPrice();
        BigDecimal targetPrice = product.getTargetPrice();

        // compareTo, not equals: equals also compares scale, so 800.0 would not equal 800.00.
        boolean targetReached = currentPrice.compareTo(targetPrice) <= 0;
        BigDecimal amountAboveTarget = targetReached
                ? MoneyUtils.normalize(BigDecimal.ZERO)
                : currentPrice.subtract(targetPrice);

        return new PriceDropResponse(
                product.getId(),
                currentPrice,
                targetPrice,
                product.getCurrency(),
                targetReached,
                amountAboveTarget);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(findProductOrThrow(id));
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
