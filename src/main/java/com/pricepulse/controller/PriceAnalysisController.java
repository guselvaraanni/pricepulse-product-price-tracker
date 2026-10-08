package com.pricepulse.controller;

import com.pricepulse.dto.PriceAnalysisResponse;
import com.pricepulse.dto.PriceTrendResponse;
import com.pricepulse.service.PriceAnalysisService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}")
public class PriceAnalysisController {

    private final PriceAnalysisService priceAnalysisService;

    public PriceAnalysisController(PriceAnalysisService priceAnalysisService) {
        this.priceAnalysisService = priceAnalysisService;
    }

    @GetMapping("/price-analysis")
    public PriceAnalysisResponse analyzePriceHistory(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "recent must be at least 1")
            @Max(value = 50, message = "recent must be at most 50") int recent) {
        return priceAnalysisService.analyzePriceHistory(productId, recent);
    }

    @GetMapping("/price-trend")
    public PriceTrendResponse analyzePriceTrend(@PathVariable Long productId) {
        return priceAnalysisService.analyzePriceTrend(productId);
    }
}
