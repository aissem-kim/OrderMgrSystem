package com.ordermgr.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderItemDto(
        Long id,
        Long productId,
        String productName,
        Long quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        LocalDateTime createdAt
) {
}
