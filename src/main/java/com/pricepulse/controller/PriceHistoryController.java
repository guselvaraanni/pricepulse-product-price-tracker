package com.pricepulse.controller;

import com.pricepulse.dto.PriceHistoryResponse;
import com.pricepulse.dto.RecordPriceRequest;
import com.pricepulse.service.PriceHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/prices")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    public PriceHistoryController(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PriceHistoryResponse recordPrice(@PathVariable Long productId,
                                            @Valid @RequestBody RecordPriceRequest request) {
        return priceHistoryService.recordPrice(productId, request);
    }

    @GetMapping
    public List<PriceHistoryResponse> getPriceHistory(@PathVariable Long productId) {
        return priceHistoryService.getPriceHistory(productId);
    }
}
