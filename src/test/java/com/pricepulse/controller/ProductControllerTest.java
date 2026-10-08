package com.pricepulse.controller;

import com.pricepulse.dto.CreateProductRequest;
import com.pricepulse.dto.ProductResponse;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Web layer only: real controller, validation and GlobalExceptionHandler; the service is a mock.
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private final MockMvc mockMvc;

    // @MockitoBean replaces the real bean in Spring's context, so it is declared on a field by design.
    @MockitoBean
    private ProductService productService;

    @Autowired
    ProductControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(new ProductResponse(
                1L, "Headphones", "https://example.com/headphones", new BigDecimal("29990.00"),
                new BigDecimal("25000.00"), "INR", true, now, now));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Headphones", "productUrl": "https://example.com/headphones",
                                 "currentPrice": 29990.00, "targetPrice": 25000.00, "currency": "INR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/products/1")))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void invalidCreateReturns400AndNeverReachesService() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Headphones", "productUrl": "https://example.com/headphones",
                                 "currentPrice": -5, "targetPrice": 25000.00, "currency": "INR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Current price must be positive"));

        verify(productService, never()).createProduct(any());
    }

    @Test
    void missingProductReturns404WithJsonError() throws Exception {
        when(productService.getProduct(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found with id 99"));
    }

    @Test
    void updateWithUnknownFieldReturns400NamingTheField() throws Exception {
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Headphones", "productUrl": "https://example.com/headphones",
                                 "targetPrice": 25000.00, "currency": "USD", "active": true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("currency")));
    }
}
