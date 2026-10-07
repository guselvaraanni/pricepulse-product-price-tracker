package com.pricepulse.service;

import com.pricepulse.dto.PriceHistoryResponse;
import com.pricepulse.dto.RecordPriceRequest;
import com.pricepulse.entity.PriceHistory;
import com.pricepulse.entity.Product;
import com.pricepulse.exception.InactiveProductException;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.repository.PriceHistoryRepository;
import com.pricepulse.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;

    public PriceHistoryService(PriceHistoryRepository priceHistoryRepository,
                               ProductRepository productRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.productRepository = productRepository;
    }

    // One transaction: the history row and the product's current price are saved together or not at all.
    @Transactional
    public PriceHistoryResponse recordPrice(Long productId, RecordPriceRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (!product.isActive()) {
            throw new InactiveProductException(productId);
        }

        PriceHistory entry = new PriceHistory(product, request.price(), LocalDateTime.now());
        PriceHistory saved = priceHistoryRepository.save(entry);

        product.updateCurrentPrice(request.price());

        return PriceHistoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }

        return priceHistoryRepository.findByProductIdOrderByRecordedAtDesc(productId).stream()
                .map(PriceHistoryResponse::from)
                .toList();
    }
}
