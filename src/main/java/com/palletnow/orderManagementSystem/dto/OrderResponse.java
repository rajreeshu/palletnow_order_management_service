package com.palletnow.orderManagementSystem.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, Long customerId, String customerName, Instant createdAt,
                            List<OrderItemResponse> items, BigDecimal totalAmount) {
    public record OrderItemResponse(Long productId, String productName, BigDecimal unitPrice,
                                    Integer quantity, BigDecimal lineTotal) {}
}
